package com.odde.donut.services.notebookGit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.TreeFormatter;

/**
 * A Portable directory: named file blobs and child directories. An unchanged child may stay as a
 * tree object id so later serialization can reuse it without opening descendants.
 */
final class NotebookGitDirectoryTree {
  private final Map<String, Entry> entries = new HashMap<>();
  private final NotebookGitAcceptedDirectoryReader reader;

  sealed interface Entry {
    record File(ObjectId blobId, FileMode mode) implements Entry {}

    record Directory(NotebookGitDirectoryTree tree) implements Entry {}

    /** An accepted child tree whose contents are not needed for this edit. */
    record TreeRef(ObjectId treeId) implements Entry {}
  }

  private record Descent(String name, String rest) {
    static Descent of(String path) {
      int slash = path.indexOf('/');
      if (slash < 0) return new Descent(path, "");
      return new Descent(path.substring(0, slash), path.substring(slash + 1));
    }

    boolean atLeaf() {
      return rest.isEmpty();
    }
  }

  private record NamedMode(String name, FileMode mode, ObjectId id) {}

  NotebookGitDirectoryTree() {
    this(null);
  }

  NotebookGitDirectoryTree(NotebookGitAcceptedDirectoryReader reader) {
    this.reader = reader;
  }

  /**
   * The accepted root: each immediate child is a file blob or an unresolved subtree id. Caller
   * keeps {@code objectReader} open for on-demand ancestor loads during this edit.
   */
  static NotebookGitDirectoryTree fromAcceptedRoot(ObjectReader objectReader, ObjectId rootTreeId) {
    return new NotebookGitAcceptedDirectoryReader(objectReader).open(rootTreeId);
  }

  void putLoaded(String name, Entry entry) {
    entries.put(name, entry);
  }

  void putFile(String path, ObjectId blobId) {
    Descent step = Descent.of(path);
    if (step.atLeaf()) {
      Entry existing = entries.get(step.name());
      if (existing instanceof Entry.File file && file.blobId().equals(blobId)) {
        return;
      }
      entries.put(step.name(), new Entry.File(blobId, FileMode.REGULAR_FILE));
      return;
    }
    directory(step.name()).putFile(step.rest(), blobId);
  }

  void removeFile(String path) {
    Descent step = Descent.of(path);
    if (step.atLeaf()) {
      entries.remove(step.name());
      return;
    }
    NotebookGitDirectoryTree child = openDirectory(step.name());
    if (child == null) return;
    child.removeFile(step.rest());
    if (child.entries.isEmpty()) entries.remove(step.name());
  }

  /** Ensures the directory at {@code prefix} exists (empty prefix is this tree). */
  void ensureDirectory(String prefix) {
    if (prefix.isEmpty()) return;
    Descent step = Descent.of(pathWithoutTrailingSlash(prefix));
    directory(step.name()).ensureDirectory(step.rest().isEmpty() ? "" : step.rest() + "/");
  }

  /**
   * Removes the directory at {@code prefix} and returns it when present. Touches only that subtree.
   */
  Optional<NotebookGitDirectoryTree> takeDirectory(String prefix) {
    if (prefix.isEmpty()) throw new IllegalArgumentException("root cannot be taken");
    Descent step = Descent.of(pathWithoutTrailingSlash(prefix));
    if (step.atLeaf()) {
      Entry removed = entries.remove(step.name());
      if (removed instanceof Entry.Directory directory) return Optional.of(directory.tree());
      if (removed instanceof Entry.TreeRef treeRef) return Optional.of(resolve(treeRef));
      return Optional.empty();
    }
    NotebookGitDirectoryTree child = openDirectory(step.name());
    if (child == null) return Optional.empty();
    Optional<NotebookGitDirectoryTree> taken =
        child.takeDirectory(step.rest().isEmpty() ? "" : step.rest() + "/");
    if (child.entries.isEmpty()) entries.remove(step.name());
    return taken;
  }

  void putDirectory(String prefix, NotebookGitDirectoryTree tree) {
    if (prefix.isEmpty()) throw new IllegalArgumentException("root cannot be replaced");
    Descent step = Descent.of(pathWithoutTrailingSlash(prefix));
    if (step.atLeaf()) {
      entries.put(step.name(), new Entry.Directory(tree));
      return;
    }
    directory(step.name()).putDirectory(step.rest().isEmpty() ? "" : step.rest() + "/", tree);
  }

  /**
   * Whether the directory at {@code prefix} exists and holds no entries other than optional {@code
   * .keep}.
   */
  boolean isEmptyAsideFromKeep(String prefix) {
    NotebookGitDirectoryTree directory = directoryAt(prefix);
    if (directory == null) return false;
    return directory.entries.keySet().stream().allMatch(".keep"::equals);
  }

  Map<String, ObjectId> toBlobIds() {
    Map<String, ObjectId> blobIds = new HashMap<>();
    collectBlobIds("", blobIds);
    return blobIds;
  }

  boolean hasUnresolvedTreeRefs() {
    return entries.values().stream()
        .anyMatch(
            entry ->
                switch (entry) {
                  case Entry.TreeRef ignored -> true;
                  case Entry.Directory directory -> directory.tree().hasUnresolvedTreeRefs();
                  case Entry.File ignored -> false;
                });
  }

  /**
   * Serializes this directory and opened descendants with JGit tree formatting; reuses unresolved
   * {@link Entry.TreeRef} ids. Returns the root tree id and stores new tree bytes in {@code trees}.
   */
  ObjectId writeTrees(Map<ObjectId, byte[]> trees) {
    List<NamedMode> sorted = new ArrayList<>();
    for (Map.Entry<String, Entry> entry : entries.entrySet()) {
      switch (entry.getValue()) {
        case Entry.File file ->
            sorted.add(new NamedMode(entry.getKey(), file.mode(), file.blobId()));
        case Entry.Directory directory ->
            sorted.add(
                new NamedMode(entry.getKey(), FileMode.TREE, directory.tree().writeTrees(trees)));
        case Entry.TreeRef treeRef ->
            sorted.add(new NamedMode(entry.getKey(), FileMode.TREE, treeRef.treeId()));
      }
    }
    sorted.sort(NotebookGitDirectoryTree::compareGitOrder);
    TreeFormatter formatter = new TreeFormatter();
    for (NamedMode entry : sorted) {
      formatter.append(entry.name(), entry.mode(), entry.id());
    }
    byte[] raw = formatter.toByteArray();
    ObjectId treeId = new ObjectInserter.Formatter().idFor(Constants.OBJ_TREE, raw);
    trees.put(treeId, raw);
    return treeId;
  }

  private static int compareGitOrder(NamedMode a, NamedMode b) {
    byte[] aBytes = a.name().getBytes(StandardCharsets.UTF_8);
    byte[] bBytes = b.name().getBytes(StandardCharsets.UTF_8);
    int aLen = FileMode.TREE.equals(a.mode()) ? aBytes.length + 1 : aBytes.length;
    int bLen = FileMode.TREE.equals(b.mode()) ? bBytes.length + 1 : bBytes.length;
    int common = Math.min(aLen, bLen);
    for (int i = 0; i < common; i++) {
      int aByte = i == aBytes.length ? '/' : (aBytes[i] & 0xff);
      int bByte = i == bBytes.length ? '/' : (bBytes[i] & 0xff);
      if (aByte != bByte) return aByte - bByte;
    }
    return aLen - bLen;
  }

  private static String pathWithoutTrailingSlash(String prefix) {
    return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
  }

  private NotebookGitDirectoryTree directoryAt(String prefix) {
    if (prefix.isEmpty()) return this;
    Descent step = Descent.of(pathWithoutTrailingSlash(prefix));
    NotebookGitDirectoryTree child = openDirectory(step.name());
    if (child == null) return null;
    return child.directoryAt(step.rest().isEmpty() ? "" : step.rest() + "/");
  }

  private NotebookGitDirectoryTree directory(String name) {
    NotebookGitDirectoryTree existing = openDirectory(name);
    if (existing != null) return existing;
    NotebookGitDirectoryTree created = new NotebookGitDirectoryTree(reader);
    entries.put(name, new Entry.Directory(created));
    return created;
  }

  private NotebookGitDirectoryTree openDirectory(String name) {
    Entry existing = entries.get(name);
    if (existing instanceof Entry.Directory directory) return directory.tree();
    if (existing instanceof Entry.TreeRef treeRef) {
      NotebookGitDirectoryTree opened = resolve(treeRef);
      entries.put(name, new Entry.Directory(opened));
      return opened;
    }
    return null;
  }

  private NotebookGitDirectoryTree resolve(Entry.TreeRef treeRef) {
    if (reader == null) {
      throw new IllegalStateException("cannot open tree ref without an accepted-tree reader");
    }
    return reader.open(treeRef.treeId());
  }

  private void collectBlobIds(String prefix, Map<String, ObjectId> blobIds) {
    for (Map.Entry<String, Entry> entry : entries.entrySet()) {
      switch (entry.getValue()) {
        case Entry.File file -> blobIds.put(prefix + entry.getKey(), file.blobId());
        case Entry.Directory directory ->
            directory.tree().collectBlobIds(prefix + entry.getKey() + "/", blobIds);
        case Entry.TreeRef treeRef ->
            throw new IllegalStateException(
                "cannot flatten unresolved tree ref " + treeRef.treeId().name());
      }
    }
  }
}

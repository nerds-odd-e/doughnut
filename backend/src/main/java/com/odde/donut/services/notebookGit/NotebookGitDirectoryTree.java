package com.odde.donut.services.notebookGit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;

/**
 * A Portable directory: named file blobs and child directories. An unchanged child may stay as a
 * tree object id so later serialization can reuse it without opening descendants.
 */
final class NotebookGitDirectoryTree {
  private final Map<String, Entry> entries = new HashMap<>();

  sealed interface Entry {
    record File(ObjectId blobId) implements Entry {}

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

  /** Eager bridge: every accepted path becomes a file under nested directories. */
  static NotebookGitDirectoryTree fromBlobIds(Map<String, ObjectId> blobIds) {
    NotebookGitDirectoryTree root = new NotebookGitDirectoryTree();
    blobIds.forEach(root::putFile);
    return root;
  }

  void putFile(String path, ObjectId blobId) {
    Descent step = Descent.of(path);
    if (step.atLeaf()) {
      entries.put(step.name(), new Entry.File(blobId));
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
    Entry child = entries.get(step.name());
    if (!(child instanceof Entry.Directory directory)) return;
    directory.tree().removeFile(step.rest());
    if (directory.tree().entries.isEmpty()) entries.remove(step.name());
  }

  /** Ensures the directory at {@code prefix} exists (empty prefix is this tree). */
  void ensureDirectory(String prefix) {
    if (prefix.isEmpty()) return;
    Descent step = Descent.of(prefix);
    directory(step.name()).ensureDirectory(step.rest());
  }

  /**
   * Removes the directory at {@code prefix} and returns it when present. Touches only that subtree.
   */
  Optional<NotebookGitDirectoryTree> takeDirectory(String prefix) {
    if (prefix.isEmpty()) throw new IllegalArgumentException("root cannot be taken");
    Descent step = Descent.of(prefix);
    if (step.atLeaf()) {
      Entry removed = entries.remove(step.name());
      if (removed instanceof Entry.Directory directory) return Optional.of(directory.tree());
      if (removed instanceof Entry.TreeRef) {
        throw new IllegalStateException("eager seed has no unresolved tree refs to take");
      }
      return Optional.empty();
    }
    Entry child = entries.get(step.name());
    if (!(child instanceof Entry.Directory directory)) return Optional.empty();
    Optional<NotebookGitDirectoryTree> taken = directory.tree().takeDirectory(step.rest());
    if (directory.tree().entries.isEmpty()) entries.remove(step.name());
    return taken;
  }

  void putDirectory(String prefix, NotebookGitDirectoryTree tree) {
    if (prefix.isEmpty()) throw new IllegalArgumentException("root cannot be replaced");
    Descent step = Descent.of(prefix);
    if (step.atLeaf()) {
      entries.put(step.name(), new Entry.Directory(tree));
      return;
    }
    directory(step.name()).putDirectory(step.rest(), tree);
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

  private NotebookGitDirectoryTree directoryAt(String prefix) {
    if (prefix.isEmpty()) return this;
    Descent step = Descent.of(prefix);
    Entry child = entries.get(step.name());
    if (!(child instanceof Entry.Directory directory)) return null;
    return directory.tree().directoryAt(step.rest());
  }

  private NotebookGitDirectoryTree directory(String name) {
    Entry existing = entries.get(name);
    if (existing instanceof Entry.Directory directory) return directory.tree();
    if (existing instanceof Entry.TreeRef) {
      throw new IllegalStateException("eager seed has no unresolved tree refs to open");
    }
    NotebookGitDirectoryTree created = new NotebookGitDirectoryTree();
    entries.put(name, new Entry.Directory(created));
    return created;
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

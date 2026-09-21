package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Reads accepted Portable tree blob ids and entries from a Git commit, and folder-row paths. */
final class NotebookGitAcceptedTree {

  private NotebookGitAcceptedTree() {}

  static List<PortableTreeEntry> readEntries(Repository repository, ObjectId commitId) {
    List<PortableTreeEntry> entries = new ArrayList<>();
    try {
      for (Map.Entry<String, ObjectId> blob : blobIds(repository, commitId).entrySet()) {
        entries.add(
            new PortableTreeEntry(blob.getKey(), repository.open(blob.getValue()).getBytes()));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read accepted Portable tree", e);
    }
    return entries;
  }

  /**
   * Each path of the tree at {@code commitId} with its Git blob id, read from tree objects alone:
   * no blob is opened, and file modes are not part of the result.
   */
  static Map<String, ObjectId> blobIds(Repository repository, ObjectId commitId) {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(revWalk.parseCommit(commitId).getTree());
      treeWalk.setRecursive(true);
      Map<String, ObjectId> blobIds = new HashMap<>();
      while (treeWalk.next()) {
        blobIds.put(treeWalk.getPathString(), treeWalk.getObjectId(0));
      }
      return blobIds;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not inspect accepted Portable tree", e);
    }
  }

  /** Each entry's path with the Git blob id of its bytes, comparable with the accepted tree's. */
  static Map<String, ObjectId> blobIds(List<PortableTreeEntry> entries) {
    ObjectInserter.Formatter formatter = new ObjectInserter.Formatter();
    return entries.stream()
        .collect(
            Collectors.toMap(
                PortableTreeEntry::path,
                entry -> formatter.idFor(Constants.OBJ_BLOB, entry.content())));
  }

  static boolean representedInTree(String folderPath, List<PortableTreeEntry> entries) {
    return representedInTree(folderPath, entries, path -> false);
  }

  static boolean representedInTree(
      String folderPath, List<PortableTreeEntry> entries, String excludingPath) {
    return representedInTree(
        folderPath, entries, path -> excludingPath != null && path.equals(excludingPath));
  }

  /**
   * True when some entry under {@code folderPath} is not itself under {@code excludingPrefix} (for
   * example tip content of a relocated destination must not invent representation for its parent).
   */
  static boolean representedInTreeExcludingUnder(
      String folderPath, List<PortableTreeEntry> entries, String excludingPrefix) {
    return representedInTree(folderPath, entries, path -> path.startsWith(excludingPrefix));
  }

  private static boolean representedInTree(
      String folderPath, List<PortableTreeEntry> entries, Predicate<String> excludedPath) {
    return entries.stream()
        .anyMatch(entry -> entry.path().startsWith(folderPath) && !excludedPath.test(entry.path()));
  }

  static Map<Integer, PortableTreeFolderRow> indexFoldersById(List<PortableTreeFolderRow> folders) {
    return folders.stream()
        .collect(Collectors.toMap(PortableTreeFolderRow::id, Function.identity()));
  }

  static String folderPath(
      PortableTreeFolderRow folder, Map<Integer, PortableTreeFolderRow> folderById) {
    String parentPath =
        folder.parentFolderId() == null
            ? ""
            : folderPath(folderById.get(folder.parentFolderId()), folderById);
    return parentPath + folder.name() + "/";
  }
}

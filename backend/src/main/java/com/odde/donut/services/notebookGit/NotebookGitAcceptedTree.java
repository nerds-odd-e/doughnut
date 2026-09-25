package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Reads accepted Portable tree blob ids and entries from a Git commit. */
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

  /** Reserved Git metadata from the accepted tip (for example {@code .gitattributes}). */
  static List<PortableTreeEntry> metadataEntries(Repository repository, ObjectId commitId) {
    return NotebookGitAttributes.selectFrom(readEntries(repository, commitId));
  }

  /** Root tree id of the commit at {@code commitId}. */
  static ObjectId rootTreeId(Repository repository, ObjectId commitId) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      return revWalk.parseCommit(commitId).getTree();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read accepted root tree id", e);
    }
  }

  /**
   * Each path of the tree at {@code commitId} with its Git blob id, read from tree objects alone:
   * no blob is opened, and file modes are not part of the result.
   */
  static Map<String, ObjectId> blobIds(Repository repository, ObjectId commitId) {
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(rootTreeId(repository, commitId));
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

  /**
   * Whether the tree at {@code commitId} has a file, or a folder of files, at a path. The tree is
   * read once, so one result can check many candidate paths.
   */
  static Predicate<String> takenPaths(Repository repository, ObjectId commitId) {
    Set<String> blobPaths = blobIds(repository, commitId).keySet();
    return path ->
        blobPaths.stream().anyMatch(taken -> taken.equals(path) || taken.startsWith(path + "/"));
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
}

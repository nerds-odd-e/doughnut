package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Reads accepted Portable tree entries from a Git commit and folder-row Portable paths. */
final class NotebookGitAcceptedTree {

  private NotebookGitAcceptedTree() {}

  static List<PortableTreeEntry> readEntries(Repository repository, ObjectId commitId) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(commitId);
      try (TreeWalk treeWalk = new TreeWalk(repository)) {
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        List<PortableTreeEntry> entries = new ArrayList<>();
        while (treeWalk.next()) {
          entries.add(
              new PortableTreeEntry(
                  treeWalk.getPathString(), repository.open(treeWalk.getObjectId(0)).getBytes()));
        }
        return sorted(entries);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not inspect accepted Portable tree", e);
    }
  }

  static List<PortableTreeEntry> sorted(List<PortableTreeEntry> entries) {
    return entries.stream().sorted(Comparator.comparing(PortableTreeEntry::path)).toList();
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

  static Map<Integer, ExportFolderRow> indexFoldersById(List<ExportFolderRow> folders) {
    return folders.stream().collect(Collectors.toMap(ExportFolderRow::id, Function.identity()));
  }

  static String folderPath(ExportFolderRow folder, Map<Integer, ExportFolderRow> folderById) {
    String parentPath =
        folder.parentFolderId() == null
            ? ""
            : folderPath(folderById.get(folder.parentFolderId()), folderById);
    return parentPath + folder.name() + "/";
  }
}

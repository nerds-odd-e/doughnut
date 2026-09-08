package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Reads accepted Portable paths and folder ancestry from a Git commit. */
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
          String content =
              new String(
                  repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8);
          entries.add(new PortableTreeEntry(treeWalk.getPathString(), content));
        }
        return entries;
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not inspect accepted Portable tree", e);
    }
  }

  static List<PortableTreeEntry> sorted(List<PortableTreeEntry> entries) {
    return entries.stream().sorted(Comparator.comparing(PortableTreeEntry::path)).toList();
  }

  static String portablePath(Note note, Map<Integer, ExportFolderRow> folderById) {
    Folder folder = note.getFolder();
    String folderPath =
        folder == null ? "" : folderPath(folderById.get(folder.getId()), folderById);
    return folderPath + note.getTitle() + ".md";
  }

  static boolean representedInAccepted(String folderPath, List<PortableTreeEntry> accepted) {
    return accepted.stream().anyMatch(entry -> entry.path().startsWith(folderPath));
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

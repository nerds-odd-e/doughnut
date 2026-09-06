package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Compares the live MySQL projection with a notebook's accepted Portable tree. */
@Service
public class NotebookGitProjection {
  public Note requireMatchingAcceptedTreeWithOneLiveNoteAtPath(
      Notebook notebook,
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      Repository repository,
      ObjectId acceptedHead,
      String changedPath) {
    requireMatchingAcceptedTree(notebook, folders, liveNotes, repository, acceptedHead);
    Map<Integer, ExportFolderRow> folderById =
        folders.stream().collect(Collectors.toMap(ExportFolderRow::id, Function.identity()));
    List<Note> matches =
        liveNotes.stream()
            .filter(note -> portablePath(note, folderById).equals(changedPath))
            .toList();
    if (matches.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one live Note at Portable path " + changedPath);
    }
    return matches.getFirst();
  }

  public void requireMatchingAcceptedTree(
      Notebook notebook,
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      Repository repository,
      ObjectId acceptedHead) {
    if (!matchesAcceptedTree(notebook, folders, liveNotes, repository, acceptedHead)) {
      throw projectionDrift();
    }
  }

  public boolean matchesAcceptedTree(
      Notebook notebook,
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      Repository repository,
      ObjectId acceptedHead) {
    List<PortableTreeEntry> currentEntries =
        PortableTreeSnapshot.build(
            notebook.getReadmeContent(), folders, NotebookExportRows.notes(liveNotes));
    return sorted(currentEntries).equals(sorted(readEntries(repository, acceptedHead)));
  }

  private static ResponseStatusException projectionDrift() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "The notebook's current Portable content differs from accepted main; refresh the checkout"
            + " before publishing.");
  }

  private static List<PortableTreeEntry> readEntries(Repository repository, ObjectId commitId) {
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

  private static List<PortableTreeEntry> sorted(List<PortableTreeEntry> entries) {
    return entries.stream().sorted(Comparator.comparing(PortableTreeEntry::path)).toList();
  }

  private static String portablePath(Note note, Map<Integer, ExportFolderRow> folderById) {
    Folder folder = note.getFolder();
    String folderPath =
        folder == null ? "" : folderPath(folderById.get(folder.getId()), folderById);
    return folderPath + note.getTitle() + ".md";
  }

  private static String folderPath(
      ExportFolderRow folder, Map<Integer, ExportFolderRow> folderById) {
    String parentPath =
        folder.parentFolderId() == null
            ? ""
            : folderPath(folderById.get(folder.parentFolderId()), folderById);
    return parentPath + folder.name() + "/";
  }
}

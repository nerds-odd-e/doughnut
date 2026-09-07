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
  /**
   * Resolves the destination folder for an added or relocated note from the accepted Portable tree.
   * Root paths return {@code null}. Nested folders, including README-only ones, are selected by
   * their full path when any tracked accepted content sits under that path.
   */
  public Integer requireRepresentedFolderId(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      String notePath) {
    int folderPathEnd = notePath.lastIndexOf('/');
    if (folderPathEnd < 0) {
      return null;
    }

    return requireRepresentedFolderPath(
        folders, repository, acceptedHead, notePath.substring(0, folderPathEnd + 1), notePath);
  }

  record RepresentedFolderRelocation(int sourceFolderId, Integer destParentFolderId) {}

  /**
   * Resolves the source Folder and destination parent of an exact folder relocation against
   * accepted Portable paths. Notebook root is a valid destination parent. Nested parents must exist
   * as folder rows and have tracked accepted content under their full path.
   */
  RepresentedFolderRelocation requireRepresentedFolderRelocation(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    Integer sourceFolderId =
        requireRepresentedFolderPath(
            folders,
            repository,
            acceptedHead,
            relocation.sourcePrefix() + "/",
            relocation.sourcePrefix() + "/README.md");
    Integer destParentFolderId =
        requireRepresentedDestinationParent(
            folders, repository, acceptedHead, relocation.destPrefix());
    return new RepresentedFolderRelocation(sourceFolderId, destParentFolderId);
  }

  private Integer requireRepresentedDestinationParent(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      String destPrefix) {
    int lastSlash = destPrefix.lastIndexOf('/');
    if (lastSlash < 0) {
      return null;
    }
    return requireRepresentedFolderPath(
        folders,
        repository,
        acceptedHead,
        destPrefix.substring(0, lastSlash + 1),
        destPrefix + "/README.md");
  }

  private Integer requireRepresentedFolderPath(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      String requiredFolderPath,
      String pathForError) {
    Map<Integer, ExportFolderRow> folderById = indexFoldersById(folders);
    ExportFolderRow folder =
        folders.stream()
            .filter(candidate -> folderPath(candidate, folderById).equals(requiredFolderPath))
            .findFirst()
            .orElseThrow(() -> unrepresentedParentFolder(pathForError));
    if (!representedInAccepted(requiredFolderPath, readEntries(repository, acceptedHead))) {
      throw unrepresentedParentFolder(pathForError);
    }
    return folder.id();
  }

  void requireNoUnrepresentedEmptySourceDescendants(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      int sourceFolderId) {
    Map<Integer, ExportFolderRow> folderById = indexFoldersById(folders);
    String sourcePath = folderPath(folderById.get(sourceFolderId), folderById);
    List<PortableTreeEntry> accepted = readEntries(repository, acceptedHead);
    for (ExportFolderRow folder : folders) {
      String descendantPath = folderPath(folder, folderById);
      if (descendantPath.equals(sourcePath) || !descendantPath.startsWith(sourcePath)) {
        continue;
      }
      if (!representedInAccepted(descendantPath, accepted)) {
        throw unrepresentedEmptyDescendant(descendantPath);
      }
    }
  }

  public Note requireOneLiveNoteAtPath(
      List<ExportFolderRow> folders, List<Note> liveNotes, String changedPath) {
    Map<Integer, ExportFolderRow> folderById = indexFoldersById(folders);
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

  private static ResponseStatusException unrepresentedEmptyDescendant(String descendantPath) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Descendant folder \""
            + descendantPath
            + "\" is not represented in accepted Portable content; every active descendant must"
            + " have tracked content before the folder can be moved.");
  }

  private static ResponseStatusException unrepresentedParentFolder(String notePath) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Parent folder for path \""
            + notePath
            + "\" is not represented in accepted Portable content; add this note at the notebook"
            + " root or inside an existing represented folder.");
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

  private static boolean representedInAccepted(
      String folderPath, List<PortableTreeEntry> accepted) {
    return accepted.stream().anyMatch(entry -> entry.path().startsWith(folderPath));
  }

  private static Map<Integer, ExportFolderRow> indexFoldersById(List<ExportFolderRow> folders) {
    return folders.stream().collect(Collectors.toMap(ExportFolderRow::id, Function.identity()));
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

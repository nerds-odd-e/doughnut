package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Compares the live MySQL projection with a notebook's accepted Portable tree. */
@Service
public class NotebookGitProjection {
  /**
   * Whether {@code folderId} is already represented in the accepted Portable tree. Notebook root
   * ({@code folderId == null}) is represented. A live folder row with no accepted descendant or
   * README is not; callers that must keep unsynchronized web behavior should not throw.
   */
  boolean isRepresentedFolder(
      Integer folderId,
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead) {
    if (folderId == null) {
      return true;
    }
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    ExportFolderRow folder = folderById.get(folderId);
    if (folder == null) {
      return false;
    }
    return NotebookGitAcceptedTree.representedInAccepted(
        NotebookGitAcceptedTree.folderPath(folder, folderById),
        NotebookGitAcceptedTree.readEntries(repository, acceptedHead));
  }

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
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    ExportFolderRow folder =
        folders.stream()
            .filter(
                candidate ->
                    NotebookGitAcceptedTree.folderPath(candidate, folderById)
                        .equals(requiredFolderPath))
            .findFirst()
            .orElseThrow(() -> unrepresentedParentFolder(pathForError));
    if (!NotebookGitAcceptedTree.representedInAccepted(
        requiredFolderPath, NotebookGitAcceptedTree.readEntries(repository, acceptedHead))) {
      throw unrepresentedParentFolder(pathForError);
    }
    return folder.id();
  }

  void requireNoUnrepresentedEmptySourceDescendants(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      int sourceFolderId) {
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    String sourcePath =
        NotebookGitAcceptedTree.folderPath(folderById.get(sourceFolderId), folderById);
    List<PortableTreeEntry> accepted =
        NotebookGitAcceptedTree.readEntries(repository, acceptedHead);
    for (ExportFolderRow folder : folders) {
      String descendantPath = NotebookGitAcceptedTree.folderPath(folder, folderById);
      if (descendantPath.equals(sourcePath) || !descendantPath.startsWith(sourcePath)) {
        continue;
      }
      if (!NotebookGitAcceptedTree.representedInAccepted(descendantPath, accepted)) {
        throw unrepresentedEmptyDescendant(descendantPath);
      }
    }
  }

  public Note requireOneLiveNoteAtPath(
      List<ExportFolderRow> folders, List<Note> liveNotes, String changedPath) {
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    List<Note> matches =
        liveNotes.stream()
            .filter(
                note -> NotebookGitAcceptedTree.portablePath(note, folderById).equals(changedPath))
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
    return NotebookGitAcceptedTree.sorted(currentEntries)
        .equals(
            NotebookGitAcceptedTree.sorted(
                NotebookGitAcceptedTree.readEntries(repository, acceptedHead)));
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

  static ResponseStatusException unrepresentedParentFolder(String notePath) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Parent folder for path \""
            + notePath
            + "\" is not represented in accepted Portable content; add this note at the notebook"
            + " root or inside an existing represented folder.");
  }
}

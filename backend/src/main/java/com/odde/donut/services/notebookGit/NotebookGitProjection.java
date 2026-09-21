package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookLivePortableTree;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
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
  private final NotebookLivePortableTree livePortableTree;

  public NotebookGitProjection(NotebookLivePortableTree livePortableTree) {
    this.livePortableTree = livePortableTree;
  }

  /**
   * Resolves the destination Folder for placing {@code notePath}. Root paths return {@code null}.
   * The parent must already be a Folder row and must be represented either in the accepted tree or
   * by other tip content (excluding the note's own path), so a relocation cannot invent
   * representation for an empty parent while an earlier-range folder Readme still can.
   */
  public Integer requireRepresentedFolderId(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      ObjectId tipHead,
      String notePath) {
    int folderPathEnd = notePath.lastIndexOf('/');
    if (folderPathEnd < 0) {
      return null;
    }
    String requiredFolderPath = notePath.substring(0, folderPathEnd + 1);
    ExportFolderRow folder = requireFolderRowAtPath(folders, requiredFolderPath, notePath);
    if (NotebookGitAcceptedTree.representedInTree(
            requiredFolderPath, NotebookGitAcceptedTree.readEntries(repository, acceptedHead))
        || NotebookGitAcceptedTree.representedInTree(
            requiredFolderPath,
            NotebookGitAcceptedTree.readEntries(repository, tipHead),
            notePath)) {
      return folder.id();
    }
    throw unrepresentedParentFolder(notePath);
  }

  record RepresentedFolderRelocation(int sourceFolderId, Integer destParentFolderId) {}

  /**
   * Resolves the source Folder of an exact folder relocation. The source must be represented in the
   * accepted tree.
   */
  int requireRepresentedRelocationSource(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    return requireRepresentedFolderPath(
        folders,
        repository,
        acceptedHead,
        relocation.sourcePrefix() + "/",
        relocation.sourcePrefix() + "/README.md");
  }

  boolean hasFolderAtPath(List<ExportFolderRow> folders, String requiredFolderPath) {
    return folderRowAtPath(folders, requiredFolderPath) != null;
  }

  /**
   * Resolves a destination parent that already exists as a folder row. Notebook root is a valid
   * destination parent. Nested parents must be represented in the accepted tree or by tip content
   * so a parent added earlier in the range can receive the relocated source. Callers construct a
   * dest parent that is absent from live folders.
   */
  Integer requireRepresentedDestinationParent(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId acceptedHead,
      ObjectId tipHead,
      String destParentPrefix,
      String destPrefix) {
    if (destParentPrefix.isEmpty()) {
      return null;
    }
    String parentPath = destParentPrefix + "/";
    String pathForError = destPrefix + "/README.md";
    ExportFolderRow folder = requireFolderRowAtPath(folders, parentPath, pathForError);
    if (NotebookGitAcceptedTree.representedInTree(
            parentPath, NotebookGitAcceptedTree.readEntries(repository, acceptedHead))
        || NotebookGitAcceptedTree.representedInTreeExcludingUnder(
            parentPath,
            NotebookGitAcceptedTree.readEntries(repository, tipHead),
            destPrefix + "/")) {
      return folder.id();
    }
    throw unrepresentedParentFolder(pathForError);
  }

  private Integer requireRepresentedFolderPath(
      List<ExportFolderRow> folders,
      Repository repository,
      ObjectId treeHead,
      String requiredFolderPath,
      String pathForError) {
    ExportFolderRow folder = requireFolderRowAtPath(folders, requiredFolderPath, pathForError);
    if (!NotebookGitAcceptedTree.representedInTree(
        requiredFolderPath, NotebookGitAcceptedTree.readEntries(repository, treeHead))) {
      throw unrepresentedParentFolder(pathForError);
    }
    return folder.id();
  }

  private static ExportFolderRow requireFolderRowAtPath(
      List<ExportFolderRow> folders, String requiredFolderPath, String pathForError) {
    ExportFolderRow folder = folderRowAtPath(folders, requiredFolderPath);
    if (folder == null) {
      throw unrepresentedParentFolder(pathForError);
    }
    return folder;
  }

  private static ExportFolderRow folderRowAtPath(
      List<ExportFolderRow> folders, String requiredFolderPath) {
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    return folders.stream()
        .filter(
            candidate ->
                NotebookGitAcceptedTree.folderPath(candidate, folderById)
                    .equals(requiredFolderPath))
        .findFirst()
        .orElse(null);
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
      if (!NotebookGitAcceptedTree.representedInTree(descendantPath, accepted)) {
        throw unrepresentedEmptyDescendant(descendantPath);
      }
    }
  }

  public Note requireOneNoteAtPath(List<Note> notes, String changedPath) {
    List<Note> matches =
        notes.stream()
            .filter(note -> NotebookGitLivePortablePath.ofNote(note).equals(changedPath))
            .toList();
    if (matches.size() != 1) {
      throw new IllegalStateException("Expected exactly one Note at Portable path " + changedPath);
    }
    return matches.getFirst();
  }

  public void requireMatchingAcceptedTree(
      Notebook notebook,
      List<ExportFolderRow> folders,
      List<Note> storedNotes,
      Repository repository,
      ObjectId acceptedHead) {
    List<PortableTreeEntry> live = livePortableTree.entriesOf(notebook, folders, storedNotes);
    if (!NotebookGitAcceptedTree.blobIds(live)
        .equals(NotebookGitAcceptedTree.blobIds(repository, acceptedHead))) {
      throw projectionDrift();
    }
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

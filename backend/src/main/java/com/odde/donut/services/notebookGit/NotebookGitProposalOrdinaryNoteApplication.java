package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteMotionService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Applies admitted ordinary-note deletions, modifications, and renames during publication. */
@Service
class NotebookGitProposalOrdinaryNoteApplication {

  private final NotebookGitProjection projection;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final NotebookGitProposalNoteAddition noteAddition;
  private final NotebookGitProposalFilenameTitle filenameTitle;
  private final NoteMotionService noteMotionService;
  private final EntityPersister entityPersister;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader notebookGitStateLoader;

  NotebookGitProposalOrdinaryNoteApplication(
      NotebookGitProjection projection,
      NoteService noteService,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalFilenameTitle filenameTitle,
      NoteMotionService noteMotionService,
      EntityPersister entityPersister,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader notebookGitStateLoader) {
    this.projection = projection;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.noteAddition = noteAddition;
    this.filenameTitle = filenameTitle;
    this.noteMotionService = noteMotionService;
    this.entityPersister = entityPersister;
    this.folderMaterialization = folderMaterialization;
    this.notebookGitStateLoader = notebookGitStateLoader;
  }

  void applyDeletions(
      NotebookGitProposalTreeShape.AdmittedShape admitted,
      List<ExportFolderRow> proposedFolders,
      List<Note> proposedLiveNotes)
      throws UnexpectedNoAccessRightException {
    for (NotebookGitProposalTreeShape.NoteChange noteChange : admitted.noteChanges()) {
      if (noteChange.kind() != NotebookGitProposalTreeShape.ChangeKind.DELETED) {
        continue;
      }
      Note deletedNote =
          projection.requireOneLiveNoteAtPath(
              proposedFolders, proposedLiveNotes, noteChange.path());
      noteService.permanentlyRemove(
          deletedNote,
          NoteTrashReferenceHandling.LEAVE_DEAD_LINKS,
          authorizationService.getCurrentUser());
      proposedLiveNotes.remove(deletedNote);
    }
  }

  List<ExportFolderRow> applyModificationsAndRenames(
      NotebookGitProposalTreeShape.AdmittedShape admitted,
      List<ExportFolderRow> proposedFolders,
      Notebook notebook,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<Note> proposedLiveNotes,
      Timestamp publishedAt) {
    List<String> renameDestinations =
        admitted.noteChanges().stream()
            .filter(change -> change.kind() == NotebookGitProposalTreeShape.ChangeKind.RENAMED)
            .map(NotebookGitProposalTreeShape.NoteChange::path)
            .toList();
    Map<String, Folder> destinationFolders =
        folderMaterialization.ensureAncestry(notebook, proposedFolders, renameDestinations);
    // Reread after materialization so every path resolves against the same folders the live notes
    // are placed in, including the folders this publication just created.
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(notebook);
    for (NotebookGitProposalTreeShape.NoteChange noteChange : admitted.noteChanges()) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
        AuthoredNoteDocument document =
            noteAddition.readValidatedDocument(proposal, noteChange.path());
        Note changedNote =
            projection.requireOneLiveNoteAtPath(folders, proposedLiveNotes, noteChange.path());
        authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.RENAMED) {
        applyRename(
            folders, destinationFolders, proposal, proposedLiveNotes, noteChange, publishedAt);
      }
    }
    return folders;
  }

  private void applyRename(
      List<ExportFolderRow> folders,
      Map<String, Folder> destinationFolders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<Note> liveNotes,
      NotebookGitProposalTreeShape.NoteChange noteChange,
      Timestamp publishedAt) {
    Note note = projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.origin().path());
    String newTitle = filenameTitle.requireValid(noteChange.path());
    Folder destinationFolder =
        noteAddition.destinationFolder(destinationFolders, noteChange.path());
    noteMotionService.assignPlacement(note, note.getNotebook(), destinationFolder, newTitle);
    note.setUpdatedAt(publishedAt);
    entityPersister.save(note);
    if (!noteChange.blobId().equals(noteChange.origin().blobId())) {
      AuthoredNoteDocument document =
          noteAddition.readValidatedDocument(proposal, noteChange.path());
      authoredNoteDocumentPersistence.persist(note, document, publishedAt);
    }
  }
}

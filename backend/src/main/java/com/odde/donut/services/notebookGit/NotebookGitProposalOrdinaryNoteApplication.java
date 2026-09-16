package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteMotionService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
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

  NotebookGitProposalOrdinaryNoteApplication(
      NotebookGitProjection projection,
      NoteService noteService,
      AuthorizationService authorizationService,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalFilenameTitle filenameTitle,
      NoteMotionService noteMotionService,
      EntityPersister entityPersister) {
    this.projection = projection;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.noteAddition = noteAddition;
    this.filenameTitle = filenameTitle;
    this.noteMotionService = noteMotionService;
    this.entityPersister = entityPersister;
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
          NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS,
          authorizationService.getCurrentUser());
      proposedLiveNotes.remove(deletedNote);
    }
  }

  void applyModificationsAndRenames(
      NotebookGitProposalTreeShape.AdmittedShape admitted,
      List<ExportFolderRow> proposedFolders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<Note> proposedLiveNotes,
      Timestamp publishedAt) {
    for (NotebookGitProposalTreeShape.NoteChange noteChange : admitted.noteChanges()) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
        AuthoredNoteDocument document =
            noteAddition.readValidatedDocument(proposal, noteChange.path());
        Note changedNote =
            projection.requireOneLiveNoteAtPath(
                proposedFolders, proposedLiveNotes, noteChange.path());
        authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.RENAMED) {
        applyRename(
            proposedFolders, proposal, acceptedHead, proposedLiveNotes, noteChange, publishedAt);
      }
    }
  }

  private void applyRename(
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<Note> liveNotes,
      NotebookGitProposalTreeShape.NoteChange noteChange,
      Timestamp publishedAt) {
    Note note = projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.origin().path());
    String newTitle = filenameTitle.requireValid(noteChange.path());
    Folder destinationFolder =
        noteAddition.representedDestinationFolder(
            folders, proposal, acceptedHead, noteChange.path());
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

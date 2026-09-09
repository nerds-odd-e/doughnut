package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.NoteFactory;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Applies one ordinary-note addition from a Git proposal: validated authored document, filename
 * title, represented destination Folder, fresh Note identity, and persistence.
 */
@Service
class NotebookGitProposalNoteAddition {

  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final EntityPersister entityPersister;
  private final NotebookGitProposalFilenameTitle filenameTitle;
  private final NoteFactory noteFactory;
  private final NotebookGitProjection projection;

  NotebookGitProposalNoteAddition(
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      EntityPersister entityPersister,
      NotebookGitProposalFilenameTitle filenameTitle,
      NoteFactory noteFactory,
      NotebookGitProjection projection) {
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.entityPersister = entityPersister;
    this.filenameTitle = filenameTitle;
    this.noteFactory = noteFactory;
    this.projection = projection;
  }

  Note apply(
      Notebook notebook,
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String path,
      Timestamp publishedAt) {
    AuthoredNoteDocument document = readValidatedDocument(proposal, path);
    String title = filenameTitle.requireValid(path);
    Folder destinationFolder = representedDestinationFolder(folders, proposal, acceptedHead, path);
    Note addedNote;
    try {
      addedNote = noteFactory.create(notebook, destinationFolder, title);
    } catch (ApiException exception) {
      throw exception.withContext("Cannot add note at path \"" + path + "\"");
    }
    authoredNoteDocumentPersistence.persist(addedNote, document, publishedAt);
    return addedNote;
  }

  Folder representedDestinationFolder(
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String path) {
    Integer destinationFolderId =
        projection.requireRepresentedFolderId(folders, proposal.repository(), acceptedHead, path);
    return destinationFolderId == null
        ? null
        : entityPersister.find(Folder.class, destinationFolderId);
  }

  AuthoredNoteDocument readValidatedDocument(
      NotebookGitProposalImporter.ImportedProposal proposal, String path) {
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
    try {
      AuthoredNoteContent.assertValidForSave(content);
    } catch (ApiException exception) {
      throw exception.withContext("Invalid authored property at path \"" + path + "\"");
    }
    return AuthoredNoteDocument.fromContent(content, canonicalDonutOrigin);
  }
}

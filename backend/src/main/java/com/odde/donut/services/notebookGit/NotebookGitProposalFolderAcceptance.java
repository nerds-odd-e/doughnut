package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderConstructionService;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Applies eligible Folder-creation or Folder-relocation proposals. */
@Service
class NotebookGitProposalFolderAcceptance {

  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final EntityPersister entityPersister;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final TestabilitySettings testabilitySettings;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final FolderConstructionService folderConstructionService;
  private final Validator validator;
  private final NotebookGitProposalNoteAddition noteAddition;
  private final NotebookGitProposalInitialNotebookReadmePublication
      initialNotebookReadmePublication;

  NotebookGitProposalFolderAcceptance(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      TestabilitySettings testabilitySettings,
      FolderSiblingNameValidation folderSiblingNameValidation,
      FolderConstructionService folderConstructionService,
      Validator validator,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalInitialNotebookReadmePublication initialNotebookReadmePublication) {
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.testabilitySettings = testabilitySettings;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.folderConstructionService = folderConstructionService;
    this.validator = validator;
    this.noteAddition = noteAddition;
    this.initialNotebookReadmePublication = initialNotebookReadmePublication;
  }

  String acceptCreation(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.RootFolderCreation creation) {
    String readmePath = creation.readmePath();
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), state.liveNotes(), proposal.repository(), acceptedHead);

    String readme = NotebookGitProposalTypedPath.requireReadme(proposal, readmePath);
    createRootFolderWithReadme(state.notebook(), readmePath, readme);

    projection.requireMatchingAcceptedTree(
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptRootFolderAndContainedNote(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.RootFolderAndContainedNoteCreation creation) {
    NotebookGitProposalTypedPath.requireOrdinaryNote(proposal, creation.notePath());
    initialNotebookReadmePublication.assertReadyEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        "Initial folder Readme and contained Note require an empty notebook.");

    String folderReadme =
        NotebookGitProposalTypedPath.requireReadme(proposal, creation.folderReadmePath());
    createRootFolderWithReadme(state.notebook(), creation.folderReadmePath(), folderReadme);
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(state.notebook());
    Note added =
        noteAddition.apply(
            state.notebook(),
            folders,
            proposal,
            proposal.mainHead(),
            creation.notePath(),
            testabilitySettings.getCurrentUTCTimestamp());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, List.of(added), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptInitialCreation(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.InitialNotebookAndRootFolderCreation creation) {
    List<ExportFolderRow> folders =
        createInitialNotebookAndRootFolder(
            state,
            proposal,
            acceptedHead,
            creation.notebookReadmePath(),
            creation.folderReadmePath());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, state.liveNotes(), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptInitialCreationWithNote(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.InitialNotebookRootFolderAndNoteCreation creation) {
    NotebookGitProposalTypedPath.requireOrdinaryNote(proposal, creation.notePath());
    List<ExportFolderRow> folders =
        createInitialNotebookAndRootFolder(
            state,
            proposal,
            acceptedHead,
            creation.notebookReadmePath(),
            creation.folderReadmePath());
    Note added =
        noteAddition.apply(
            state.notebook(),
            folders,
            proposal,
            proposal.mainHead(),
            creation.notePath(),
            testabilitySettings.getCurrentUTCTimestamp());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, List.of(added), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  private List<ExportFolderRow> createInitialNotebookAndRootFolder(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String notebookReadmePath,
      String folderReadmePath) {
    initialNotebookReadmePublication.storeOnEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        notebookReadmePath,
        "Initial notebook and folder Readmes require an empty notebook.");
    String folderReadme = NotebookGitProposalTypedPath.requireReadme(proposal, folderReadmePath);
    createRootFolderWithReadme(state.notebook(), folderReadmePath, folderReadme);
    return notebookGitStateLoader.foldersOf(state.notebook());
  }

  private void createRootFolderWithReadme(Notebook notebook, String readmePath, String readme) {
    String folderName = readmePath.substring(0, readmePath.indexOf('/'));
    FolderCreationRequest request = validRootFolderRequest(readmePath, folderName);
    Folder folder = folderConstructionService.createFolder(notebook, request);
    folder.setReadmeContent(readme);
    entityPersister.save(folder);
    entityPersister.flush();
  }

  String accept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    List<ExportFolderRow> folders = state.folders();
    NotebookGitProjection.RepresentedFolderRelocation represented =
        projection.requireRepresentedFolderRelocation(
            folders, proposal.repository(), acceptedHead, relocation);
    projection.requireNoUnrepresentedEmptySourceDescendants(
        folders, proposal.repository(), acceptedHead, represented.sourceFolderId());
    NotebookGitProposalFolderPlacement.requireAllowed(
        represented, entityPersister, folderSiblingNameValidation, relocation.destPrefix());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, state.liveNotes(), proposal.repository(), acceptedHead);
    Folder source = entityPersister.find(Folder.class, represented.sourceFolderId());
    source.setParentFolder(
        represented.destParentFolderId() == null
            ? null
            : entityPersister.find(Folder.class, represented.destParentFolderId()));
    entityPersister.save(source);
    entityPersister.flush();
    projection.requireMatchingAcceptedTree(
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  private FolderCreationRequest validRootFolderRequest(String path, String folderName) {
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName(folderName);
    Set<ConstraintViolation<FolderCreationRequest>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      String reason =
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .sorted()
              .findFirst()
              .orElseThrow();
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid folder name at path \"" + path + "\": " + reason);
    }
    return request;
  }
}

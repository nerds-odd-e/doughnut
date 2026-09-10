package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies eligible Folder-relocation proposals. */
@Service
class NotebookGitProposalFolderRelocation {

  private final NotebookGitProjection projection;
  private final EntityPersister entityPersister;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final FolderSiblingNameValidation folderSiblingNameValidation;

  NotebookGitProposalFolderRelocation(
      NotebookGitProjection projection,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      FolderSiblingNameValidation folderSiblingNameValidation) {
    this.projection = projection;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
  }

  NotebookGitStateLoader.LockedNotebookState apply(
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
    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(),
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes());
  }
}

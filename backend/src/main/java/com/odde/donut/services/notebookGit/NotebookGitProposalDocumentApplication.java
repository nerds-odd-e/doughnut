package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangedDocument;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.DocumentRole;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies admitted folder, README and concept additions from classified proposal documents. */
@Service
class NotebookGitProposalDocumentApplication {

  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader stateLoader;
  private final EntityPersister entityPersister;
  private final NotebookGitProposalNoteAddition noteAddition;

  NotebookGitProposalDocumentApplication(
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader,
      EntityPersister entityPersister,
      NotebookGitProposalNoteAddition noteAddition) {
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
    this.entityPersister = entityPersister;
    this.noteAddition = noteAddition;
  }

  NotebookGitStateLoader.LockedNotebookState apply(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<ChangedDocument> documents,
      Timestamp publishedAt) {
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    String notebookReadmePath = null;
    List<String> folderReadmePaths = new ArrayList<>();
    List<String> conceptPaths = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.role() == DocumentRole.CONCEPT) {
        conceptPaths.add(document.path());
      } else if ("README.md".equals(document.path())) {
        notebookReadmePath = document.path();
      } else {
        folderReadmePaths.add(document.path());
      }
    }
    if (notebookReadmePath != null) {
      storeReadme(state, proposal, notebookReadmePath);
    }
    Map<String, Folder> materializedFolders =
        folderMaterialization.materialize(
            state.notebook(),
            state.folders(),
            proposal.repository(),
            ObjectId.fromString(state.binding().getAcceptedGitObjectId()),
            Stream.concat(folderReadmePaths.stream(), conceptPaths.stream()).toList(),
            proposal);
    List<ExportFolderRow> folders = stateLoader.foldersOf(state.notebook());
    List<Note> notes = new ArrayList<>(state.liveNotes());
    for (String path : conceptPaths) {
      notes.add(
          noteAddition.apply(state.notebook(), materializedFolders, proposal, path, publishedAt));
    }
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(), state.notebook(), folders, notes);
  }

  private void storeReadme(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String notebookReadmePath) {
    state
        .notebook()
        .setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, notebookReadmePath));
    entityPersister.save(state.notebook());
  }
}

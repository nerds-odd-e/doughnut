package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Makes the live projection exactly match a proposed Portable tree before accepting it. */
@Service
class NotebookGitProposalAcceptance {

  private final NotebookGitProjection projection;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final TestabilitySettings testabilitySettings;
  private final EntityPersister entityPersister;

  NotebookGitProposalAcceptance(
      NotebookGitProjection projection,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister) {
    this.projection = projection;
    this.repositoryStore = repositoryStore;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
  }

  String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    return acceptMatchingProposedTree(
        published, proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal,
      Timestamp publishedAt) {
    NotebookGitStateLoader.LockedNotebookState reconciled =
        requireMatchingProposedTree(published, proposal);
    return repositoryStore.store(reconciled.binding(), proposal.repository(), publishedAt);
  }

  NotebookGitStateLoader.LockedNotebookState requireMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    NotebookGitStateLoader.LockedNotebookState reconciled =
        reconcileUnrepresentedFolders(published, proposal);
    projection.requireMatchingAcceptedTree(
        reconciled.notebook(),
        reconciled.folders(),
        reconciled.storedNotes(),
        proposal.repository(),
        proposal.mainHead());
    return reconciled;
  }

  private NotebookGitStateLoader.LockedNotebookState reconcileUnrepresentedFolders(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    List<ExportFolderRow> folders = published.folders();
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    List<PortableTreeEntry> proposedEntries =
        NotebookGitAcceptedTree.readEntries(proposal.repository(), proposal.mainHead());
    List<ExportFolderRow> unrepresented =
        folders.stream()
            .filter(
                folder ->
                    !NotebookGitAcceptedTree.representedInTree(
                        NotebookGitAcceptedTree.folderPath(folder, folderById), proposedEntries))
            .sorted(
                Comparator.comparingInt(
                        (ExportFolderRow folder) ->
                            NotebookGitAcceptedTree.folderPath(folder, folderById).length())
                    .reversed())
            .toList();
    for (ExportFolderRow folder : unrepresented) {
      entityPersister.remove(entityPersister.find(Folder.class, folder.id()));
    }
    Set<Integer> removedIds =
        unrepresented.stream().map(ExportFolderRow::id).collect(Collectors.toSet());
    return new NotebookGitStateLoader.LockedNotebookState(
        published.binding(),
        published.notebook(),
        folders.stream().filter(folder -> !removedIds.contains(folder.id())).toList(),
        published.storedNotes());
  }
}

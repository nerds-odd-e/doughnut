package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
  private final NotebookAttachmentRepository attachmentRepository;

  NotebookGitProposalAcceptance(
      NotebookGitProjection projection,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NotebookAttachmentRepository attachmentRepository) {
    this.projection = projection;
    this.repositoryStore = repositoryStore;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.attachmentRepository = attachmentRepository;
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
    projectRootAttachments(published.notebook(), proposal);
    NotebookGitStateLoader.LockedNotebookState reconciled =
        requireMatchingProposedTree(published, proposal);
    return repositoryStore.store(reconciled.binding(), proposal.repository(), publishedAt);
  }

  /**
   * Makes the notebook's stored root Attachments exactly the proposed tip's set. One final-set rule
   * covers addition, edit, rename and removal: no commit is replayed. A filename the tip keeps is
   * updated in place, so removals and insertions never share a filename and the per-notebook
   * filename key cannot trip. This runs before the tip comparison, so the compared tree is the
   * complete post-mutation result.
   */
  private void projectRootAttachments(
      Notebook notebook, NotebookGitProposalImporter.ImportedProposal proposal) {
    Map<String, byte[]> proposed = new HashMap<>();
    for (PortableTreeEntry entry :
        NotebookGitAcceptedTree.readEntries(proposal.repository(), proposal.mainHead())) {
      if (NotebookGitProposalTreeShape.isRootAttachment(entry.path())) {
        proposed.put(entry.path(), entry.content());
      }
    }
    for (NotebookAttachment stored : attachmentRepository.findByNotebook_Id(notebook.getId())) {
      byte[] content = proposed.remove(stored.getFilename());
      if (content == null) {
        entityPersister.remove(stored);
      } else if (!Arrays.equals(stored.getContent(), content)) {
        stored.setContent(content);
        entityPersister.save(stored);
      }
    }
    proposed.forEach((filename, content) -> persistAttachment(notebook, filename, content));
    entityPersister.flush();
  }

  private void persistAttachment(Notebook notebook, String filename, byte[] content) {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFilename(filename);
    attachment.setContent(content);
    entityPersister.save(attachment);
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

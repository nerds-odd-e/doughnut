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
  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader stateLoader;

  NotebookGitProposalAcceptance(
      NotebookGitProjection projection,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NotebookAttachmentRepository attachmentRepository,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader) {
    this.projection = projection;
    this.repositoryStore = repositoryStore;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.attachmentRepository = attachmentRepository;
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
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
    NotebookGitStateLoader.LockedNotebookState withAttachmentFolders =
        projectAttachments(published, proposal);
    NotebookGitStateLoader.LockedNotebookState reconciled =
        requireMatchingProposedTree(withAttachmentFolders, proposal);
    return repositoryStore.store(reconciled.binding(), proposal.repository(), publishedAt);
  }

  /**
   * Makes the notebook's stored Attachments exactly the proposed tip's set. One final-set rule
   * covers addition, edit, rename and removal: no commit is replayed. A path the tip keeps is
   * updated in place, so removals and insertions never share a sibling filename and its uniqueness
   * key cannot trip. This runs before the tip comparison, so the compared tree is the complete
   * post-mutation result.
   */
  private NotebookGitStateLoader.LockedNotebookState projectAttachments(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    Notebook notebook = published.notebook();
    Map<String, byte[]> proposed = new HashMap<>();
    for (PortableTreeEntry entry :
        NotebookGitAcceptedTree.readEntries(proposal.repository(), proposal.mainHead())) {
      if (NotebookGitProposalTreeShape.isAttachment(entry.path())) {
        proposed.put(entry.path(), entry.content());
      }
    }
    Map<String, Folder> foldersByPath =
        folderMaterialization.ensureAncestry(notebook, proposed.keySet().stream().toList());
    entityPersister.flush();
    List<ExportFolderRow> folders = stateLoader.foldersOf(notebook);
    Map<Integer, ExportFolderRow> folderById = NotebookGitAcceptedTree.indexFoldersById(folders);
    for (NotebookAttachment stored : attachmentRepository.findByNotebook_Id(notebook.getId())) {
      byte[] content = proposed.remove(attachmentPath(stored, folderById));
      if (content == null) {
        entityPersister.remove(stored);
      } else if (!Arrays.equals(stored.getContent(), content)) {
        stored.setContent(content);
        entityPersister.save(stored);
      }
    }
    proposed.forEach((path, content) -> persistAttachment(notebook, foldersByPath, path, content));
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        published.binding(), notebook, folders, published.storedNotes());
  }

  private static String attachmentPath(
      NotebookAttachment attachment, Map<Integer, ExportFolderRow> folderById) {
    Folder folder = attachment.getFolder();
    String folderPath =
        folder == null
            ? ""
            : NotebookGitAcceptedTree.folderPath(folderById.get(folder.getId()), folderById);
    return folderPath + attachment.getFilename();
  }

  private void persistAttachment(
      Notebook notebook, Map<String, Folder> foldersByPath, String path, byte[] content) {
    int separator = path.lastIndexOf('/');
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFolder(separator < 0 ? null : foldersByPath.get(path.substring(0, separator)));
    attachment.setFilename(path.substring(separator + 1));
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

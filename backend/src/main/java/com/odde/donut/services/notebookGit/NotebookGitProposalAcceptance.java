package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.NotePropertyIndexService;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;
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
  private final NotePropertyIndexService notePropertyIndexService;

  NotebookGitProposalAcceptance(
      NotebookGitProjection projection,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NotebookAttachmentRepository attachmentRepository,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader,
      NotePropertyIndexService notePropertyIndexService) {
    this.projection = projection;
    this.repositoryStore = repositoryStore;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.attachmentRepository = attachmentRepository;
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
    this.notePropertyIndexService = notePropertyIndexService;
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
    dateChangedValues(reconciled, proposal);
    return repositoryStore.store(
        reconciled.binding(), proposal.repository(), proposal.mainHead(), publishedAt);
  }

  /**
   * Dates each pushed note's values at the committer time of the commit, between the accepted head
   * and the proposed head, that last changed them; the projection has already refreshed the notes'
   * index rows, and a value edited and reverted inside the range takes the revert's time.
   */
  private void dateChangedValues(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    NotebookGitPropertyValueDating.dateValues(
            proposal.repository(),
            ObjectId.fromString(published.binding().getAcceptedGitObjectId()),
            proposal.mainHead())
        .stream()
        .collect(Collectors.groupingBy(NotebookGitPropertyValueDating.ValueDate::tipPath))
        .forEach((path, dates) -> {
          Note note = projection.requireOneNoteAtPath(published.storedNotes(), path);
          dates.forEach(date ->
              notePropertyIndexService.writeValueDate(note, date.valueKey(), date.committedAt()));
        });
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
      if (PortablePathKind.of(entry.path()) == PortablePathKind.ATTACHMENT) {
        proposed.put(entry.path(), entry.content());
      }
    }
    Map<String, Folder> foldersByPath =
        folderMaterialization.ensureAncestry(notebook, proposed.keySet().stream().toList());
    entityPersister.flush();
    List<PortableTreeFolderRow> folders = stateLoader.foldersOf(notebook);
    Map<Integer, String> prefixes = NotebookGitPortablePath.folderPrefixes(folders);
    for (NotebookAttachment stored : attachmentRepository.findByNotebook_Id(notebook.getId())) {
      byte[] content = proposed.remove(attachmentPath(stored, prefixes));
      if (content == null) {
        entityPersister.remove(stored);
      } else if (!Arrays.equals(stored.getAcceptedGitContent(), content)) {
        stored.setAcceptedGitContent(content);
        entityPersister.save(stored);
      }
    }
    proposed.forEach((path, content) -> persistAttachment(notebook, foldersByPath, path, content));
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        published.binding(), notebook, folders, published.storedNotes());
  }

  private static String attachmentPath(
      NotebookAttachment attachment, Map<Integer, String> prefixes) {
    Folder folder = attachment.getFolder();
    return NotebookGitPortablePath.ofAttachment(
        prefixes.get(folder == null ? null : folder.getId()), attachment.getFilename());
  }

  private void persistAttachment(
      Notebook notebook, Map<String, Folder> foldersByPath, String path, byte[] content) {
    int separator = path.lastIndexOf('/');
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFolder(separator < 0 ? null : foldersByPath.get(path.substring(0, separator)));
    attachment.setFilename(path.substring(separator + 1));
    attachment.setAcceptedGitContent(content);
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
    List<PortableTreeFolderRow> folders = published.folders();
    Map<Integer, String> prefixes = NotebookGitPortablePath.folderPrefixes(folders);
    List<PortableTreeEntry> proposedEntries =
        NotebookGitAcceptedTree.readEntries(proposal.repository(), proposal.mainHead());
    List<PortableTreeFolderRow> unrepresented =
        folders.stream()
            .filter(folder ->
                !NotebookGitAcceptedTree.representedInTree(
                    prefixes.get(folder.id()), proposedEntries))
            .sorted(
                Comparator.comparingInt(
                        (PortableTreeFolderRow folder) -> prefixes.get(folder.id()).length())
                    .reversed())
            .toList();
    for (PortableTreeFolderRow folder : unrepresented) {
      entityPersister.remove(entityPersister.find(Folder.class, folder.id()));
    }
    Set<Integer> removedIds =
        unrepresented.stream().map(PortableTreeFolderRow::id).collect(Collectors.toSet());
    return new NotebookGitStateLoader.LockedNotebookState(
        published.binding(),
        published.notebook(),
        folders.stream().filter(folder -> !removedIds.contains(folder.id())).toList(),
        published.storedNotes());
  }
}

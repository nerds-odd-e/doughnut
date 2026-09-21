package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class NotebookGitAttachmentPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  private static final String CHANGED_REFERENCE_JSON = "{\"schema\": \"changed\"}\n";
  // Deliberately not valid UTF-8, so nothing may decode these bytes on the way in or out.
  private static final byte[] LOWERCASE_DIAGRAM = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};
  private static final byte[] CAPITALIZED_DIAGRAM = {(byte) 0x80, 0x00, (byte) 0xC3};

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired FolderRepository folderRepository;

  @Test
  void publishedRootFilesAreTheExactAcceptedTipAndSurviveTheNextWebNoteSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    ObjectId initialHead = ObjectId.fromString(empty.getAcceptedGitObjectId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("reference.json", REFERENCE_JSON),
                new NotebookGitProposalFile("Diagram.png", CAPITALIZED_DIAGRAM),
                new NotebookGitProposalFile("diagram.png", LOWERCASE_DIAGRAM))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(published.parent(), is(initialHead));
    assertThat(
        published.entries(),
        contains(
            new PortableTreeEntry("Diagram.png", CAPITALIZED_DIAGRAM),
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("diagram.png", LOWERCASE_DIAGRAM),
            PortableTreeEntry.ofText("reference.json", REFERENCE_JSON)));

    Note storedNote = noteRepository.findById(committedNoteIds(notebook).getFirst()).orElseThrow();
    textContentController.updateNoteContent(storedNote, contentDto(EDITED_CONTENT));

    AcceptedTip afterWebSave = acceptedTip(notebook);
    assertThat(afterWebSave.parent(), is(published.head()));
    assertThat(
        afterWebSave.entries(),
        contains(
            new PortableTreeEntry("Diagram.png", CAPITALIZED_DIAGRAM),
            PortableTreeEntry.ofText("Root Note.md", EDITED_CONTENT),
            new PortableTreeEntry("diagram.png", LOWERCASE_DIAGRAM),
            PortableTreeEntry.ofText("reference.json", REFERENCE_JSON)));
  }

  @Test
  void anInitialPublicationMayBeFileOnlyWithNoNoteAtAll() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty, List.of(new NotebookGitProposalFile("reference.json", REFERENCE_JSON))));

    assertThat(
        acceptedTip(notebook).entries(),
        contains(PortableTreeEntry.ofText("reference.json", REFERENCE_JSON)));
    assertThat(committedNoteIds(notebook), empty());
    assertThat(countFoldersForNotebook(notebook.getId()), is(0L));
  }

  @Test
  void anInitialPublicationMayContainOnlyAFileInNestedFolders() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    String attachmentPath = "tools/cache/reference.json";

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty, List.of(new NotebookGitProposalFile(attachmentPath, REFERENCE_JSON))));

    assertThat(
        acceptedTip(notebook).entries(),
        contains(PortableTreeEntry.ofText(attachmentPath, REFERENCE_JSON)));
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    Folder tools = folders.get(0);
    assertThat(tools.getName(), equalTo("tools"));
    assertThat(tools.getParentFolderId(), nullValue());
    assertThat(tools.getReadmeContent(), nullValue());
    Folder cache = folders.get(1);
    assertThat(cache.getName(), equalTo("cache"));
    assertThat(cache.getParentFolderId(), equalTo(tools.getId()));
    assertThat(cache.getReadmeContent(), nullValue());
  }

  @Test
  void invalidMarkdownBesideAValidFileChangeLeavesTheHeadAndTheFilesUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = publishNoteAndRootFile(notebook);
    List<PortableTreeEntry> filesBefore = committedRootAttachments(notebook);

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile("Root Note.md", "no frontmatter at all"),
                new NotebookGitProposalFile("reference.json", CHANGED_REFERENCE_JSON))),
        HttpStatus.BAD_REQUEST);

    assertThat(committedRootAttachments(notebook), equalTo(filesBefore));
  }

  @Test
  void aStaleHeadLeavesTheAcceptedFilesUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = publishNoteAndRootFile(notebook);
    List<PortableTreeEntry> filesBefore = committedRootAttachments(notebook);

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        ObjectId.zeroId().getName(),
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("reference.json", CHANGED_REFERENCE_JSON))),
        HttpStatus.CONFLICT);

    assertThat(committedRootAttachments(notebook), equalTo(filesBefore));
  }

  @Test
  void publishedFileBesideANoteIsExactAndSurvivesTheNextWebNoteSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    NotebookGitBinding notesOnly = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        notesOnly.getAcceptedGitObjectId(),
        proposalBundleBytes(
            notesOnly,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("physics/diagrams/Force.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("physics/diagrams/force.png", LOWERCASE_DIAGRAM))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(
        published.entries(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            PortableTreeEntry.ofText("physics/diagrams/Force.md", NOTE_MARKDOWN),
            new PortableTreeEntry("physics/diagrams/force.png", LOWERCASE_DIAGRAM)));

    Note force =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(note -> note.getTitle().equals("Force"))
            .findFirst()
            .orElseThrow();
    textContentController.updateNoteContent(force, contentDto(EDITED_CONTENT));

    assertThat(
        acceptedTip(notebook).entries(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            PortableTreeEntry.ofText("physics/diagrams/Force.md", EDITED_CONTENT),
            new PortableTreeEntry("physics/diagrams/force.png", LOWERCASE_DIAGRAM)));
  }

  /** An accepted tip holding one Markdown note and one root file, for refusal cases to preserve. */
  private NotebookGitBinding publishNoteAndRootFile(Notebook notebook) throws Exception {
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("reference.json", REFERENCE_JSON))));
    return reloadCommittedBinding(notebook.getId());
  }

  private List<Integer> committedNoteIds(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                .map(Note::getId)
                .toList());
  }

  private List<PortableTreeEntry> committedRootAttachments(Notebook notebook) {
    return NotebookLiveProjectionTestReader.rootAttachments(
        transactionManager, notebookAttachmentRepository, notebook.getId());
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(GitBundleTestReader.fetchHead(repository, downloaded));
      return new AcceptedTip(
          commit.getId(),
          commit.getParent(0).getId(),
          GitBundleTestReader.readTreeEntries(repository, commit));
    }
  }

  private record AcceptedTip(ObjectId head, ObjectId parent, List<PortableTreeEntry> entries) {}
}

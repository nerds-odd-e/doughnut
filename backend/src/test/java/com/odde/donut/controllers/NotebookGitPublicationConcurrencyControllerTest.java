package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Verifies that web saves and publications share one serialized accepted-writer contract. */
class NotebookGitPublicationConcurrencyControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE_PATH = "note.md";
  private static final String FIRST_WEB_CONTENT = "---\ntype: Note\n---\nfirst web edit";
  private static final String SECOND_WEB_CONTENT = "---\ntype: Note\n---\nsecond web edit";
  private static final String PUBLISHED_CONTENT = "---\ntype: Note\n---\npublished edit";
  private static final String AFTER_PUBLICATION_CONTENT =
      "---\ntype: Note\n---\nweb edit after publication";

  @Test
  void twoQueuedWebSavesAppendInAcceptedOrder() throws Exception {
    Fixture fixture = fixture();

    NotebookGitConcurrentWriterTestSupport.Result<NoteRealm, NoteRealm> race =
        queuedWriters(
            fixture.notebook().getId(),
            () -> saveContent(fixture.note().getId(), FIRST_WEB_CONTENT),
            () -> saveContent(fixture.note().getId(), SECOND_WEB_CONTENT));

    assertThat(race.first().getNote().getContent(), is(FIRST_WEB_CONTENT));
    assertThat(race.second().getNote().getContent(), is(SECOND_WEB_CONTENT));
    assertAcceptedHistory(
        fixture,
        List.of(SECOND_WEB_CONTENT, FIRST_WEB_CONTENT, ACCEPTED_CONTENT),
        SECOND_WEB_CONTENT);
  }

  @Test
  void webSaveQueuedFirstMakesTheCompetingPublicationStale() throws Exception {
    Fixture fixture = fixture();
    Proposal proposal = proposal(fixture.acceptedBinding(), PUBLISHED_CONTENT);

    NotebookGitConcurrentWriterTestSupport.Result<NoteRealm, PublicationAttempt> race =
        queuedWriters(
            fixture.notebook().getId(),
            () -> saveContent(fixture.note().getId(), FIRST_WEB_CONTENT),
            () -> publish(fixture, proposal));

    assertThat(race.first().getNote().getContent(), is(FIRST_WEB_CONTENT));
    assertThat(race.second().acceptedHead(), is((String) null));
    assertThat(race.second().rejection().getStatusCode(), is(HttpStatus.CONFLICT));
    assertThat(
        race.second().rejection().getReason(), containsString("expectedHead no longer matches"));
    assertAcceptedHistory(fixture, List.of(FIRST_WEB_CONTENT, ACCEPTED_CONTENT), FIRST_WEB_CONTENT);
  }

  @Test
  void publicationQueuedFirstBecomesTheParentOfTheCompetingWebSave() throws Exception {
    Fixture fixture = fixture();
    Proposal proposal = proposal(fixture.acceptedBinding(), PUBLISHED_CONTENT);

    NotebookGitConcurrentWriterTestSupport.Result<PublicationAttempt, NoteRealm> race =
        queuedWriters(
            fixture.notebook().getId(),
            () -> publish(fixture, proposal),
            () -> saveContent(fixture.note().getId(), AFTER_PUBLICATION_CONTENT));

    assertThat(race.first().rejection(), is((ResponseStatusException) null));
    assertThat(race.first().acceptedHead(), is(proposal.head().getName()));
    assertThat(race.second().getNote().getContent(), is(AFTER_PUBLICATION_CONTENT));
    assertAcceptedHistory(
        fixture,
        List.of(AFTER_PUBLICATION_CONTENT, PUBLISHED_CONTENT, ACCEPTED_CONTENT),
        AFTER_PUBLICATION_CONTENT);
  }

  private Fixture fixture() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    return new Fixture(
        notebook, note, binding, ObjectId.fromString(binding.getAcceptedGitObjectId()));
  }

  private NoteRealm saveContent(Integer noteId, String content) throws Exception {
    Note note = noteRepository.findById(noteId).orElseThrow();
    return textContentController.updateNoteContent(note, contentDto(content));
  }

  private PublicationAttempt publish(Fixture fixture, Proposal proposal) throws Exception {
    try {
      String head =
          controller.publishNotebookGitProposal(
              fixture.notebook().getId(),
              fixture.acceptedBinding().getAcceptedGitObjectId(),
              proposal.bundleBytes());
      return new PublicationAttempt(head, null);
    } catch (ResponseStatusException rejection) {
      return new PublicationAttempt(null, rejection);
    }
  }

  private <F, S> NotebookGitConcurrentWriterTestSupport.Result<F, S> queuedWriters(
      Integer notebookId, Callable<F> firstCall, Callable<S> secondCall) throws Exception {
    return NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
        transactionManager,
        notebookGitBindingRepository,
        currentUser,
        currentUser.getUser(),
        notebookId,
        firstCall,
        secondCall);
  }

  private void assertAcceptedHistory(
      Fixture fixture, List<String> newestToOldestContent, String expectedDatabaseContent)
      throws Exception {
    AcceptedState state =
        inCommittedTransaction(
            transactionManager,
            () -> {
              NotebookGitBinding binding =
                  notebookGitBindingRepository
                      .findByNotebook_Id(fixture.notebook().getId())
                      .orElseThrow();
              Note note = noteRepository.findById(fixture.note().getId()).orElseThrow();
              return new AcceptedState(
                  binding.getAcceptedGitObjectId(), binding.getBundleBytes(), note.getContent());
            });
    assertThat(state.databaseContent(), is(expectedDatabaseContent));

    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, state.bundleBytes());
      assertThat(head.getName(), is(state.acceptedHead()));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(head);
        for (int index = 0; index < newestToOldestContent.size(); index++) {
          String expectedContent = newestToOldestContent.get(index);
          assertThat(
              NotebookGitProposalBlobText.readUtf8(repository, commit, NOTE_PATH),
              is(expectedContent));
          if (index == newestToOldestContent.size() - 1) {
            assertThat(commit.getId(), equalTo(fixture.acceptedHead()));
            assertThat(commit.getParentCount(), is(0));
          } else {
            assertThat(commit.getParentCount(), is(1));
            commit = revWalk.parseCommit(commit.getParent(0));
          }
        }
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, head, NOTE_PATH),
          is(state.databaseContent()));
    }
  }

  private Proposal proposal(NotebookGitBinding binding, String content) throws Exception {
    byte[] bundleBytes =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(NOTE_PATH, content)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      return new Proposal(bundleBytes, GitBundleTestReader.fetchHead(repository, bundleBytes));
    }
  }

  private record Fixture(
      Notebook notebook, Note note, NotebookGitBinding acceptedBinding, ObjectId acceptedHead) {}

  private record Proposal(byte[] bundleBytes, ObjectId head) {}

  private record PublicationAttempt(String acceptedHead, ResponseStatusException rejection) {}

  private record AcceptedState(String acceptedHead, byte[] bundleBytes, String databaseContent) {}
}

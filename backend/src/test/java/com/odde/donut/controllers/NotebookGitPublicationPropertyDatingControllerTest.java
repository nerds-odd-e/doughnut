package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A CLI push dates each pushed note's values at the committer time of the commit in the accepted
 * range that last changed them, never earlier than the note's creation.
 */
class NotebookGitPublicationPropertyDatingControllerTest extends NotebookGitControllerTestBase {

  private static final Timestamp CREATED = Timestamp.from(Instant.parse("2026-01-01T07:00:00Z"));
  private static final Instant AT_0800 = Instant.parse("2026-01-01T08:00:00Z");
  private static final Instant AT_0900 = Instant.parse("2026-01-01T09:00:00Z");
  private static final Instant AT_0915 = Instant.parse("2026-01-01T09:15:00Z");
  private static final Timestamp PUBLISHED = Timestamp.from(Instant.parse("2026-01-01T09:30:00Z"));
  private static final Instant AT_1000 = Instant.parse("2026-01-01T10:00:00Z");

  private static final NotebookGitProposalFile N1 = note("N1", "", "N1 body");
  private static final NotebookGitProposalFile N2 = note("N2", "topic: chemistry\n", "N2 body");
  private static final NotebookGitProposalFile N3 = note("N3", "", "N3 body");
  private static final NotebookGitProposalFile PALETTE =
      note("Palette", "colors: [alpha, beta]\norigin: lab\n", "Palette body");

  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;

  private Notebook notebook;

  @BeforeEach
  void publishedNotebook() throws Exception {
    notebook = createGitBackedNotebook();
    testabilitySettings.timeTravelTo(CREATED);
    push(List.of(new PushedCommit(Instant.now(), List.of(N1, N2, N3, PALETTE))));
    testabilitySettings.timeTravelTo(PUBLISHED);
  }

  @Test
  void eachValueIsDatedAtTheCommitThatLastChangedIt() throws Exception {
    Timestamp n2TopicBefore = changedAt("N2", "topic", "");
    Timestamp betaBefore = changedAt("Palette", "colors", "beta");

    push(
        List.of(
            new PushedCommit(
                AT_0900,
                List.of(
                    note("N1", "topic: physics\n", "N1 body"),
                    N2,
                    N3,
                    note("Palette", "colors: [beta]\norigin: field\n", "Palette body"))),
            new PushedCommit(
                AT_0915,
                List.of(
                    note("N1", "topic: physics\n", "N1 body"),
                    note("N2", "topic: chemistry\n", "N2 edited"),
                    N3,
                    note("Palette", "colors: [beta]\norigin: lab\n", "Palette body"))),
            new PushedCommit(
                AT_1000,
                List.of(
                    note("N1", "topic: physics\n", "N1 body"),
                    note("N2", "topic: chemistry\n", "N2 edited"),
                    note("N3", "topic: biology\n", "N3 body"),
                    note("Palette", "colors: [beta, alpha]\norigin: lab\n", "Palette body")))));

    assertThat(changedAt("N1", "topic", ""), equalTo(Timestamp.from(AT_0900)));
    assertThat(changedAt("N3", "topic", ""), equalTo(Timestamp.from(AT_1000)));
    assertThat(changedAt("N2", "topic", ""), equalTo(n2TopicBefore));
    assertThat(changedAt("Palette", "colors", "alpha"), equalTo(Timestamp.from(AT_1000)));
    assertThat(changedAt("Palette", "colors", "beta"), equalTo(betaBefore));
    assertThat(changedAt("Palette", "origin", ""), equalTo(Timestamp.from(AT_0915)));
  }

  @Test
  void anEditRevertedInTheSamePushIsDatedAtTheRevert() throws Exception {
    push(
        List.of(
            new PushedCommit(
                AT_0900,
                List.of(
                    N1,
                    N2,
                    N3,
                    note("Palette", "colors: [alpha, beta]\norigin: field\n", "Palette body"))),
            new PushedCommit(AT_0915, List.of(N1, N2, N3, PALETTE))));

    assertThat(changedAt("Palette", "origin", ""), equalTo(Timestamp.from(AT_0915)));
  }

  @Test
  void anEditRevertedInAPushThatOnlyMovesAFolderIsDatedAtTheRevert() throws Exception {
    NotebookGitProposalFile topicsReadme = new NotebookGitProposalFile("Topics/README.md", README);
    NotebookGitProposalFile topicsNote = new NotebookGitProposalFile("Topics/A.md", NOTE);
    NotebookGitProposalFile archiveReadme =
        new NotebookGitProposalFile(
            "Archive/README.md", PortableTreeReadmeMarkdown.assemble("Archive landing"));
    push(
        List.of(
            new PushedCommit(
                AT_0800, List.of(N1, N2, N3, PALETTE, archiveReadme, topicsReadme, topicsNote))));

    push(
        List.of(
            new PushedCommit(
                AT_0900,
                List.of(
                    N1,
                    N2,
                    N3,
                    note("Palette", "colors: [alpha, beta]\norigin: field\n", "Palette body"),
                    archiveReadme,
                    topicsReadme,
                    topicsNote)),
            new PushedCommit(
                AT_0915,
                List.of(
                    N1,
                    N2,
                    N3,
                    PALETTE,
                    archiveReadme,
                    new NotebookGitProposalFile("Archive/Topics/README.md", README),
                    new NotebookGitProposalFile("Archive/Topics/A.md", NOTE)))));

    assertThat(changedAt("Palette", "origin", ""), equalTo(Timestamp.from(AT_0915)));
  }

  @Test
  void aNoteCommittedBeforeItsPublicationIsDatedAtItsCreation() throws Exception {
    push(
        List.of(
            new PushedCommit(
                AT_0800, List.of(N1, N2, N3, PALETTE, note("Fresh", "topic: new\n", "body")))));

    assertThat(noteTitled("Fresh").getCreatedAt(), equalTo(PUBLISHED));
    assertThat(changedAt("Fresh", "topic", ""), equalTo(PUBLISHED));
  }

  @Test
  void aPushChangingOnlyAnAttachmentWritesNoDates() throws Exception {
    List<Timestamp> datesBefore = allChangedAts();
    byte[] picture = pointerFor(notebook, "picture".getBytes(StandardCharsets.UTF_8));

    push(
        List.of(
            new PushedCommit(
                AT_0900,
                List.of(
                    N1, N2, N3, PALETTE, new NotebookGitProposalFile("picture.bin", picture)))));

    assertThat(allChangedAts(), equalTo(datesBefore));
  }

  private record PushedCommit(Instant committedAt, List<NotebookGitProposalFile> files) {}

  private static NotebookGitProposalFile note(String title, String properties, String body) {
    return new NotebookGitProposalFile(
        title + ".md", "---\ntype: Note\n" + properties + "---\n" + body + "\n");
  }

  private void push(List<PushedCommit> commits) throws Exception {
    String acceptedHead = reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      for (PushedCommit commit : commits) {
        head =
            commitOnTopOf(
                repository,
                List.of(head),
                withAcceptedMetadata(repository, head, commit.files()),
                "commit",
                commit.committedAt());
      }
      controller.publishNotebookGitProposal(
          notebook.getId(), acceptedHead, bundleBytesForHead(repository, head));
    }
  }

  private Note noteTitled(String title) {
    return noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .filter(note -> note.getTitle().equals(title))
        .findFirst()
        .orElseThrow();
  }

  private Timestamp changedAt(String title, String key, String value) {
    return notePropertyIndexRepository
        .findByNote_IdAndPropertyKeyAndPropertyValue(noteTitled(title).getId(), key, value)
        .getFirst()
        .getChangedAt();
  }

  private List<Timestamp> allChangedAts() {
    List<Timestamp> dates = new ArrayList<>();
    for (Note note : noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId())) {
      notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
          .map(NotePropertyIndex::getChangedAt)
          .forEach(dates::add);
    }
    return dates;
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.odde.donut.services.notebookGit.NotebookGitPropertyValueDating;
import com.odde.donut.services.notebookGit.NotebookGitPropertyValueDating.ValueDate;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Commits carry committer times in the past while their author time is the current time, so every
 * dated value below shows the committer time is the one used.
 */
class NotebookGitPropertyValueDatingTest extends NotebookGitCommitFixtureTestSupport {

  private static final Instant AT_0800 = Instant.parse("2026-01-01T08:00:00Z");
  private static final Instant AT_0900 = Instant.parse("2026-01-01T09:00:00Z");
  private static final Instant AT_0930 = Instant.parse("2026-01-01T09:30:00Z");
  private static final Instant AT_1000 = Instant.parse("2026-01-01T10:00:00Z");

  private final InMemoryRepository repository =
      new InMemoryRepository(new DfsRepositoryDescription());

  @AfterEach
  void closeRepository() {
    repository.close();
  }

  private ObjectId commit(ObjectId parent, Instant committedAt, NotebookGitProposalFile... files)
      throws IOException {
    return commitOnTopOf(
        repository,
        parent == null ? List.of() : List.of(parent),
        List.of(files),
        "commit",
        committedAt);
  }

  private static NotebookGitProposalFile note(String path, String content) {
    return new NotebookGitProposalFile(path, content);
  }

  @Test
  void datesEachValueAtTheCommitThatAddedItAndLeavesUnchangedValuesUndated() throws IOException {
    ObjectId root =
        commit(
            null,
            AT_0800,
            note("n1.md", "N1 body"),
            note("n2.md", "---\ntopic: chemistry\n---\nN2 body"),
            note("n3.md", "N3 body"));
    ObjectId c1 =
        commit(
            root,
            AT_0900,
            note("n1.md", "---\ntopic: physics\n---\nN1 body"),
            note("n2.md", "---\ntopic: chemistry\n---\nN2 body"),
            note("n3.md", "N3 body"));
    ObjectId c2 =
        commit(
            c1,
            AT_0930,
            note("n1.md", "---\ntopic: physics\n---\nN1 body"),
            note("n2.md", "---\ntopic: chemistry\n---\nN2 edited"),
            note("n3.md", "N3 body"));
    ObjectId c3 =
        commit(
            c2,
            AT_1000,
            note("n1.md", "---\ntopic: physics\n---\nN1 body"),
            note("n2.md", "---\ntopic: chemistry\n---\nN2 edited"),
            note("n3.md", "---\ntopic: biology\n---\nN3 body"));

    assertThat(
        NotebookGitPropertyValueDating.dateValues(repository, root, c3),
        containsInAnyOrder(
            new ValueDate("n1.md", "topic", "", Optional.of(AT_0900)),
            new ValueDate("n3.md", "topic", "", Optional.of(AT_1000))));
  }

  @Test
  void datesAListItemRemovedAndReAddedAtItsReAddition() throws IOException {
    ObjectId root = commit(null, AT_0800, note("n1.md", "---\ncolors: [alpha, beta]\n---\n"));
    ObjectId c1 = commit(root, AT_0900, note("n1.md", "---\ncolors: [beta]\n---\n"));
    ObjectId c2 = commit(c1, AT_0930, note("n1.md", "---\ncolors: [beta]\n---\nedited"));
    ObjectId c3 = commit(c2, AT_1000, note("n1.md", "---\ncolors: [beta, alpha]\n---\nedited"));

    assertThat(
        NotebookGitPropertyValueDating.dateValues(repository, root, c3),
        containsInAnyOrder(new ValueDate("n1.md", "colors", "alpha", Optional.of(AT_1000))));
  }

  @Test
  void followsARenamedNoteBackToTheCommitThatChangedItsValue() throws IOException {
    String content = "---\norigin: lab\n---\nA body long enough to be recognised as the same note.";
    ObjectId root = commit(null, AT_0800, note("old.md", "A body long enough."));
    ObjectId c1 = commit(root, AT_0900, note("old.md", content));
    ObjectId c2 = commit(c1, AT_0930, note("folder/new.md", content));

    assertThat(
        NotebookGitPropertyValueDating.dateValues(repository, root, c2),
        containsInAnyOrder(new ValueDate("folder/new.md", "origin", "", Optional.of(AT_0900))));
  }

  @Test
  void reportsValuesAlreadyInTheRootCommitAsPresentAtRootIncludingNotesUnchangedSince()
      throws IOException {
    ObjectId root =
        commit(
            null,
            AT_0800,
            note("n1.md", "---\ntopic: physics\n---\n"),
            note("n2.md", "---\norigin: lab\n---\n"),
            note("README.md", "---\ntopic: notebook\n---\n"));
    ObjectId c1 =
        commit(
            root,
            AT_0900,
            note("n1.md", "---\ntopic: physics\ncolors: [x]\n---\n"),
            note("n2.md", "---\norigin: lab\n---\n"),
            note("README.md", "---\ntopic: notebook\n---\n"));

    assertThat(
        NotebookGitPropertyValueDating.dateValues(repository, null, c1),
        containsInAnyOrder(
            new ValueDate("n1.md", "topic", "", Optional.empty()),
            new ValueDate("n1.md", "colors", "x", Optional.of(AT_0900)),
            new ValueDate("n2.md", "origin", "", Optional.empty())));
  }
}

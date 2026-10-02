package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.NumberedPropertyMigration;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationCrossNotebookControllerTest
    extends NotebookGitWebContentControllerTestBase {
  private static final String OLD_ITEM = "Read [[migration target:Carrier#prop:topic%202|detail]]";
  private static final String NEW_ITEM = "Read [[migration target:Carrier#prop:topic|detail]]";
  private static final String TARGET = "---\ntype: Note\ntopic: A\ntopic 2: B\n---\nCarrier";
  private static final String CONSOLIDATED = "---\ntype: Note\ntopic: [\"A\", \"B\"]\n---\nCarrier";
  private static final String SOURCE =
      "---\ntype: Note\nabout: ['" + OLD_ITEM + "', '" + NEW_ITEM + "']\n---\nSource";
  private static final String REWRITTEN = SOURCE.replace(OLD_ITEM, NEW_ITEM);
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController trackerController;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotePropertyIndexRepository propertyIndexRepository;

  @Test
  void completeSystemOperationPublishesBothOwnersTreesAndRetainsExistingReferrerLearning()
      throws Exception {
    User targetOwner = currentUser.getUser();
    Notebook targetNotebook = createGitBackedNotebook("Migration Target");
    Note target = makeMe.aNote().notebook(targetNotebook).title("Carrier").content(TARGET).please();
    Integer targetTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
                    .propertyKey("topic 2")
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please()
                    .getId());
    snapshotCurrentPortableTree(targetNotebook);
    AcceptedHistory targetBefore = acceptedHistory(targetNotebook);
    Learning targetLearning = learning(targetTracker);

    User sourceOwner = inCommittedTransaction(transactionManager, this::createFixtureUser);
    currentUser.setUser(sourceOwner);
    Notebook sourceNotebook;
    Note source;
    List<Integer> sourceTrackers;
    AcceptedHistory sourceBefore;
    Learning sourceLearning;
    try {
      sourceNotebook = createGitBackedNotebook("References");
      source = makeMe.aNote().notebook(sourceNotebook).title("Source").content(SOURCE).please();
      sourceTrackers =
          inCommittedTransaction(
              transactionManager,
              () -> {
                makeMe
                    .aBazaarNotebook(
                        notebookRepository.findById(sourceNotebook.getId()).orElseThrow())
                    .please();
                Note stored = noteRepository.findById(source.getId()).orElseThrow();
                return List.of(
                    makeMe
                        .aMemoryTrackerFor(stored)
                        .propertyKey("about")
                        .propertyValue(OLD_ITEM)
                        .afterNthStrictRecall(3)
                        .recallCount(3)
                        .please()
                        .getId(),
                    makeMe
                        .aMemoryTrackerFor(stored)
                        .propertyKey("about")
                        .propertyValue(NEW_ITEM)
                        .afterNthStrictRecall(2)
                        .recallCount(2)
                        .please()
                        .getId());
              });
      sourceLearning = learning(sourceTrackers.get(1));
      snapshotCurrentPortableTree(sourceNotebook);
      sourceBefore = acceptedHistory(sourceNotebook);
      assertThat(noteController.showNote(source).getWikiLinks(), empty());
    } finally {
      currentUser.setUser(targetOwner);
    }
    assertThat(
        noteController.showNote(source).getWikiLinks().stream()
            .map(WikiLink::getDestinationNoteId)
            .toList(),
        contains(target.getId(), target.getId()));

    assertThat(
        migration.migrateNotebook(
            targetNotebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            assertThat(
                noteController
                    .showNote(noteRepository.findById(target.getId()).orElseThrow())
                    .getNote()
                    .getContent(),
                equalTo(CONSOLIDATED));
            Note storedSource = noteRepository.findById(source.getId()).orElseThrow();
            var links = noteController.showNote(storedSource).getWikiLinks();
            assertThat(
                links.stream().map(WikiLink::getDestinationNoteId).toList(),
                contains(target.getId()));
            assertThat(
                links.stream().map(WikiLink::getTarget).toList(),
                contains("migration target:Carrier#prop:topic"));
            assertThat(
                AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, storedSource).stream()
                    .map(AuthoredNoteReferenceRow::getAuthoredLink)
                    .toList(),
                contains("migration target:Carrier#prop:topic|detail"));
            assertThat(
                propertyIndexRepository.findByNote_IdOrderByIdAsc(source.getId()).stream()
                    .map(
                        index ->
                            new PropertyFocus(index.getPropertyKey(), index.getPropertyValue()))
                    .toList(),
                contains(new PropertyFocus("about", NEW_ITEM)));
            assertThat(
                memoryTrackerRepository.findById(targetTracker).orElseThrow().propertyFocus(),
                equalTo(new PropertyFocus("topic", "B")));
            assertThat(memoryTrackerRepository.existsById(sourceTrackers.get(0)), equalTo(false));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
    assertThat(learning(targetTracker), equalTo(targetLearning));
    assertDescendant(targetNotebook, targetBefore, "Carrier.md", CONSOLIDATED);
    currentUser.setUser(sourceOwner);
    try {
      inCommittedTransaction(
          transactionManager,
          () -> {
            try {
              Note stored = noteRepository.findById(source.getId()).orElseThrow();
              var shown = noteController.showNote(stored);
              assertThat(shown.getNote().getContent(), equalTo(REWRITTEN));
              assertThat(shown.getWikiLinks(), empty());
              assertThat(
                  noteController.getNoteInfo(stored).getMemoryTrackers().stream()
                      .map(MemoryTracker::getId)
                      .toList(),
                  contains(sourceTrackers.get(1)));
              assertThat(
                  trackerController
                      .showMemoryTracker(
                          memoryTrackerRepository.findById(sourceTrackers.get(1)).orElseThrow())
                      .propertyFocus(),
                  equalTo(new PropertyFocus("about", NEW_ITEM)));
            } catch (Exception exception) {
              throw new IllegalStateException(exception);
            }
          });
      assertThat(learning(sourceTrackers.get(1)), equalTo(sourceLearning));
      assertDescendant(sourceNotebook, sourceBefore, "Source.md", REWRITTEN);
    } finally {
      currentUser.setUser(targetOwner);
    }
  }

  private void assertDescendant(
      Notebook notebook, AcceptedHistory before, String path, String content) throws Exception {
    var after = acceptedHistory(notebook);
    assertThat(after.commits(), hasSize(before.commits().size() + 1));
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(after.tipPaths(), contains(path));
    assertThat(tipText(after, path), equalTo(content));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk reader = new RevWalk(repository)) {
      var tip =
          reader.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)));
      assertThat(
          tip.getAuthorIdent().getName(), equalTo(NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME));
      assertThat(tip.getFullMessage(), equalTo("Consolidate numbered properties"));
    }
  }

  private Learning learning(Integer id) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          MemoryTracker tracker = memoryTrackerRepository.findById(id).orElseThrow();
          try {
            MemoryTracker shown = trackerController.showMemoryTracker(tracker);
            return new Learning(
                shown.getNextRecallAt(),
                shown.getLastRecalledAt(),
                shown.getAssimilatedAt(),
                shown.getStability(),
                shown.getDifficulty(),
                shown.getRemovedFromTracking(),
                objectMapper.writeValueAsString(trackerController.getRecallHistory(shown)));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  private record Learning(
      Timestamp next,
      Timestamp last,
      Timestamp assimilated,
      Float stability,
      Float difficulty,
      Boolean removed,
      String history) {}
}

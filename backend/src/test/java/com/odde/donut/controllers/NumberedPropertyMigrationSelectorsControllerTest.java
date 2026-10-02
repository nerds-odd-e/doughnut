package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationSelectorsControllerTest
    extends NotebookGitWebContentControllerTestBase {
  private static final String ORIGINAL_ITEM = "Read [[Target#prop:topic%202|detail]]";
  private static final String REWRITTEN_ITEM = "Read [[Target#prop:topic|detail]]";
  private static final String SOURCE =
      "---\ntype: Note\nabout: ['"
          + ORIGINAL_ITEM
          + "', 'unchanged']\n"
          + "summary: '[[/Target.md#prop:topic%202]]'\n---\n"
          + "[[Target#prop:topic%202|read]] [[target#prop:topic%202|case]] "
          + "[[Twin#prop:topic%202|ambiguous]] [[Missing#prop:topic%202]] [[Target#prop:wrong]]";
  private static final String REWRITTEN_SOURCE =
      "---\ntype: Note\nabout: ['"
          + REWRITTEN_ITEM
          + "', 'unchanged']\n"
          + "summary: '[[/Target.md#prop:topic|/Target.md#prop:topic%202]]'\n---\n"
          + "[[Target#prop:topic|read]] [[target#prop:topic|case]] "
          + "[[Twin#prop:topic%202|ambiguous]] [[Missing#prop:topic%202]] [[Target#prop:wrong]]";
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotePropertyIndexRepository propertyIndexRepository;

  @Test
  void resolvedSelectorsAndLearnedSourceItemsFollowWithoutChangingVisibleText() throws Exception {
    var notebook = createGitBackedNotebook();
    Note target = makeMe.aNote().notebook(notebook).title("Target").aliases("Twin").please();
    Note other = makeMe.aNote().notebook(notebook).title("Other").aliases("Twin").please();
    String targetContent = "---\ntype: Note\naliases: [Twin]\ntopic 2: A\n---\nTarget body";
    textContentController.updateNoteContent(target, contentDto(targetContent));
    textContentController.updateNoteContent(other, contentDto(targetContent.replace("A", "B")));
    Note source = makeMe.aNote().notebook(notebook).title("Source").content(SOURCE).please();
    List<MemoryTracker> trackers =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note storedTarget = noteRepository.findById(target.getId()).orElseThrow();
              Note storedSource = noteRepository.findById(source.getId()).orElseThrow();
              return List.of(
                  makeMe
                      .aMemoryTrackerFor(storedTarget)
                      .propertyKey("topic 2")
                      .afterNthStrictRecall(2)
                      .please(),
                  makeMe
                      .aMemoryTrackerFor(storedSource)
                      .propertyKey("about")
                      .propertyValue(ORIGINAL_ITEM)
                      .afterNthStrictRecall(2)
                      .please(),
                  makeMe
                      .aMemoryTrackerFor(storedSource)
                      .propertyKey("about")
                      .propertyValue("unchanged")
                      .afterNthStrictRecall(3)
                      .please());
            });
    var learningBefore = learning(trackers);
    snapshotCurrentPortableTree(notebook);
    var before = acceptedHistory(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Note stored = noteRepository.findById(source.getId()).orElseThrow();
            var shown = noteController.showNote(stored);
            assertThat(shown.getNote().getContent(), equalTo(REWRITTEN_SOURCE));
            var resolved =
                shown.getWikiLinks().stream()
                    .filter(link -> link.getResolution() == WikiLink.Resolution.RESOLVED)
                    .toList();
            assertThat(
                resolved.stream().map(WikiLink::getDestinationNoteId).toList(),
                contains(target.getId(), target.getId(), target.getId(), target.getId()));
            assertThat(
                resolved.stream().map(WikiLink::getDisplayText).toList(),
                contains("detail", "/Target.md#prop:topic%202", "read", "case"));
            assertThat(
                shown.getWikiLinks().stream()
                    .filter(link -> link.getResolution() == WikiLink.Resolution.AMBIGUOUS)
                    .map(WikiLink::getAuthoredLink)
                    .toList(),
                contains("Twin#prop:topic%202|ambiguous"));
            assertThat(
                AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, stored).stream()
                    .map(AuthoredNoteReferenceRow::getAuthoredLink)
                    .toList(),
                contains(
                    "Target#prop:topic|detail",
                    "/Target.md#prop:topic|/Target.md#prop:topic%202",
                    "Target#prop:topic|read",
                    "target#prop:topic|case",
                    "Twin#prop:topic%202|ambiguous",
                    "Missing#prop:topic%202",
                    "Target#prop:wrong"));
            assertThat(
                trackers.stream()
                    .collect(
                        Collectors.toMap(
                            MemoryTracker::getId,
                            tracker ->
                                memoryTrackerRepository
                                    .findById(tracker.getId())
                                    .orElseThrow()
                                    .propertyFocus())),
                equalTo(
                    Map.of(
                        trackers.get(0).getId(), new PropertyFocus("topic", "A"),
                        trackers.get(1).getId(), new PropertyFocus("about", REWRITTEN_ITEM),
                        trackers.get(2).getId(), new PropertyFocus("about", "unchanged"))));
            assertThat(
                propertyIndexRepository.findByNote_IdOrderByIdAsc(source.getId()).stream()
                    .map(
                        index ->
                            new PropertyFocus(index.getPropertyKey(), index.getPropertyValue()))
                    .toList(),
                containsInAnyOrder(
                    new PropertyFocus("about", REWRITTEN_ITEM),
                    new PropertyFocus("about", "unchanged"),
                    new PropertyFocus("summary", "")));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
    assertThat(learning(trackers), equalTo(learningBefore));
    var after = acceptedHistory(notebook);
    assertThat(after.commits(), hasSize(before.commits().size() + 1));
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(tipText(after, "Source.md"), equalTo(REWRITTEN_SOURCE));
    assertThat(
        tipText(after, "Target.md"),
        equalTo(targetContent.replace("topic 2: A", "topic: [\"A\"]")));
  }

  private Map<Integer, String> learning(List<MemoryTracker> trackers) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            trackers.stream()
                .collect(
                    Collectors.toMap(
                        MemoryTracker::getId,
                        tracker -> {
                          try {
                            MemoryTracker stored =
                                memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
                            return objectMapper.writeValueAsString(
                                List.of(
                                    stored.getNextRecallAt(),
                                    stored.getLastRecalledAt(),
                                    stored.getStability(),
                                    stored.getDifficulty(),
                                    memoryTrackerController.getRecallHistory(stored)));
                          } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                          }
                        })));
  }
}

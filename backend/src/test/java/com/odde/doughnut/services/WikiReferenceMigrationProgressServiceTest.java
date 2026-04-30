package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.WikiReferenceMigrationProgressRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiReferenceMigrationProgressServiceTest {

  static final String STEP = "wiki_reference_migration_test";

  @Autowired MakeMe makeMe;
  @Autowired WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;
  @Autowired WikiReferenceMigrationProgressRepository wikiReferenceMigrationProgressRepository;

  @Test
  void startOrResume_creates_row_then_pending_returns_all_ids_in_order() {
    Note a = makeMe.aNote().please();
    Note b = makeMe.aNote().please();
    List<Integer> ids = sortedIds(a, b);

    wikiReferenceMigrationProgressService.startOrResume(STEP, ids.size());

    assertThat(
        wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(STEP, ids), equalTo(ids));
  }

  @Test
  void after_batch_flushAndClear_pending_skips_ids_not_greater_than_cursor() {
    Note n1 = makeMe.aNote().please();
    Note n2 = makeMe.aNote().please();
    Note n3 = makeMe.aNote().please();
    List<Integer> ids = sortedIds(n1, n2, n3);

    wikiReferenceMigrationProgressService.startOrResume(STEP, ids.size());
    wikiReferenceMigrationProgressService.recordBatchSuccess(STEP, n2.getId(), 2);
    makeMe.entityPersister.flushAndClear();

    assertThat(
        wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(STEP, ids),
        equalTo(List.of(n3.getId())));
  }

  @Test
  void completed_step_yields_empty_pending_list() {
    Note n1 = makeMe.aNote().please();
    List<Integer> ids = List.of(n1.getId());

    wikiReferenceMigrationProgressService.startOrResume(STEP, 1);
    wikiReferenceMigrationProgressService.recordBatchSuccess(STEP, n1.getId(), 1);
    wikiReferenceMigrationProgressService.markCompleted(STEP);

    assertThat(
        wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(STEP, ids), equalTo(List.of()));
  }

  @Test
  void startOrResume_after_batch_does_not_reset_cursor() {
    Note n1 = makeMe.aNote().please();
    Note n2 = makeMe.aNote().please();
    List<Integer> ids = sortedIds(n1, n2);

    wikiReferenceMigrationProgressService.startOrResume(STEP, ids.size());
    wikiReferenceMigrationProgressService.recordBatchSuccess(STEP, n1.getId(), 1);
    makeMe.entityPersister.flushAndClear();

    wikiReferenceMigrationProgressService.startOrResume(STEP, ids.size());

    assertThat(
        wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(STEP, ids),
        equalTo(List.of(n2.getId())));
  }

  @Test
  void markFailed_keeps_processed_count_and_sets_status() {
    Note n1 = makeMe.aNote().please();
    wikiReferenceMigrationProgressService.startOrResume(STEP, 5);
    wikiReferenceMigrationProgressService.recordBatchSuccess(STEP, n1.getId(), 1);
    makeMe.entityPersister.flushAndClear();

    wikiReferenceMigrationProgressService.markFailed(STEP, "boom");

    WikiReferenceMigrationProgress p =
        wikiReferenceMigrationProgressRepository.findByStepName(STEP).orElseThrow();
    assertThat(p.getStatus(), is(WikiReferenceMigrationStepStatus.FAILED));
    assertThat(p.getProcessedCount(), equalTo(1));
    assertThat(p.getLastError(), equalTo("boom"));
  }

  private static List<Integer> sortedIds(Note... notes) {
    return java.util.Arrays.stream(notes)
        .map(Note::getId)
        .sorted(Comparator.naturalOrder())
        .toList();
  }
}

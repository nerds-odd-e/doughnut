package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves that {@link NoteTitlePlacementRules#requireNoSoftDeletedTitleAt} still sees a
 * same-transaction, not-yet-flushed soft-delete after the lookup query switched to {@code
 * FlushModeType.COMMIT} (skipping the auto-flush of unrelated pending inserts that otherwise
 * dominates bulk publication). {@link NoteService#destroy} triggers its own auto-flush via a plain
 * repository query (loading the note's memory trackers) before returning, so the pending
 * soft-delete is already visible in the database by the time this check runs, with no intervening
 * query required here. This test calls destroy() then the check with nothing in between, against
 * the real Hibernate/MySQL Unit Test database, to prove that assumption holds.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteTitlePlacementRulesFlushVisibilityTest {

  @Autowired private MakeMe makeMe;
  @Autowired private NoteService noteService;
  @Autowired private NoteTitlePlacementRules noteTitlePlacementRules;

  @Test
  void seesASameTransactionSoftDeleteWithNoInterveningQuery() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note = makeMe.aNote().notebook(notebook).title("Flush visibility").please();

    noteService.destroy(note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, user);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
                    notebook, null, "Flush visibility"));

    assertThat(
        exception.getErrorBody().getErrorType(),
        equalTo(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        exception.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(note.getId())));
  }
}

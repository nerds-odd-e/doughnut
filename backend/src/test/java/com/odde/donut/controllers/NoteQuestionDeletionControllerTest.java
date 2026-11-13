package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteQuestionDeletionControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired NoteController noteController;
  @Autowired NotebookController notebookController;
  @Autowired EntityManager entityManager;

  @Test
  void removesAnUnmovedTrackerWithItsPromptsAndQuestion() throws Exception {
    Note note = noteWithCompleteDependentData();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt prompt =
        makeMe.aRecallPrompt().forMemoryTracker(tracker).answerChoiceIndex(0).recorded().please();
    addPromptDependents(prompt);
    Integer questionId = prompt.getMcq().getId();

    permanentlyDelete(note);

    assertThat(entityManager.find(Note.class, note.getId()), nullValue());
    assertThat(entityManager.find(MemoryTracker.class, tracker.getId()), nullValue());
    assertThat(entityManager.find(RecallPrompt.class, prompt.getId()), nullValue());
    assertThat(entityManager.find(Mcq.class, questionId), nullValue());
  }

  @Test
  void removesAQuestionsCurrentTrackerNoteWhileItsInputNoteRemains() throws Exception {
    Note input = ownedNote();
    MemoryTracker tracker = ownedTracker(input);
    RecallPrompt prompt =
        makeMe.aRecallPrompt().forMemoryTracker(tracker).answerChoiceIndex(1).recorded().please();
    addPromptDependents(prompt);
    Integer questionId = prompt.getMcq().getId();
    Note current = reifyTrackedProperty(tracker, "Current owned question");
    MemoryTracker retainedTracker = ownedTracker(input);
    RecallPrompt retained =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(retainedTracker)
            .answerChoiceIndex(0)
            .recorded()
            .please();

    permanentlyDelete(current);

    assertThat(entityManager.find(Mcq.class, questionId), nullValue());
    assertThat(entityManager.find(Note.class, input.getId()), notNullValue());
    var retainedHistory =
        controller
            .getRecallHistory(entityManager.find(MemoryTracker.class, retainedTracker.getId()))
            .getFirst();
    assertThat(retainedHistory.getRecallPrompt().getId(), equalTo(retained.getId()));
    assertThat(
        retainedHistory.getRecallPrompt().getMcq().getId(), equalTo(retained.getMcq().getId()));
  }

  private void permanentlyDelete(Note note) throws Exception {
    Integer noteId = note.getId();
    noteController.trashNote(note, leaveDeadLinks());
    makeMe.entityPersister.flushAndClear();
    noteController.permanentlyDeleteNote(entityManager.find(Note.class, noteId));
    makeMe.entityPersister.flushAndClear();
  }

  private void addPromptDependents(RecallPrompt prompt) {
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(makeMe.aQuestionGenerationBatch().forUser(currentUser.getUser()).please())
        .memoryTracker(prompt.getMemoryTracker())
        .please();
    Conversation conversation =
        makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();
    makeMe
        .aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please())
        .acceptedBy(currentUser.getUser())
        .please();
  }

  private Note noteWithCompleteDependentData() throws Exception {
    var notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    makeMe.aNote("Reference target").notebook(notebook).please();
    NoteCreationDTO creation = new NoteCreationDTO();
    creation.setNewTitle("Owned learning note");
    creation.setContent(
        "---\ntype: Note\naliases: [Former title]\nnote_level: 2\n"
            + "topic: cells\nexample of: \"[[Reference target]]\"\n---\nBody\n");
    Note note = notebookController.createNoteAtNotebookRoot(notebook, creation).getNote();
    makeMe.anAssimilationSequenceSkipFor(note).please();
    Conversation conversation =
        makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
    makeMe
        .aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please())
        .acceptedBy(currentUser.getUser())
        .please();
    makeMe.entityPersister.flush();
    for (String table :
        List.of(
            "assimilation_sequence_skip",
            "conversation",
            "conversation_note_correction",
            "note_alias_index",
            "note_creator",
            "note_level_index",
            "note_property_index")) {
      assertThat(countNoteRows(table, "note_id", note.getId()), greaterThan(0L));
    }
    assertThat(
        countNoteRows("authored_note_reference", "source_note_id", note.getId()), greaterThan(0L));
    return note;
  }

  private long countNoteRows(String table, String noteColumn, Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + noteColumn + " = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }
}

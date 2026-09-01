package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.AudioUploadDTO;
import com.odde.donut.controllers.dto.NoteRefinementLayoutSelectionRequestDTO;
import com.odde.donut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.services.book.BookService;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ratchet on the "OpenAI HTTP call inside an open DB transaction" bug class. {@code CANDIDATES} is
 * every controller/service method that transitively reaches {@code OpenAiApiHandler}'s HTTP methods
 * (requestAndGetStructuredResponseResult, streamResponseAsLegacyChatChunks, getTranscription) —
 * hand-traced from the call graph, not batch JSON parsing, so no ArchUnit dependency is needed.
 * {@code ALLOWLIST} is the subset that is currently, knowingly, {@code @Transactional}: this
 * documents known violators rather than fixing them. Adding {@code @Transactional} to a candidate
 * that is not allowlisted fails this test; removing {@code @Transactional} from an allowlisted
 * method without shrinking the allowlist also fails it.
 */
class OpenAiTransactionalRatchetTest {

  private static Method method(Class<?> type, String name, Class<?>... paramTypes) {
    try {
      Method method = type.getDeclaredMethod(name, paramTypes);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Expected method not found: " + type.getName() + "#" + name, e);
    }
  }

  // Every method that transitively calls an OpenAiApiHandler HTTP method, transactional or not.
  private static final Set<Method> CANDIDATES =
      new LinkedHashSet<>(
          Set.of(
              // AiNoteAutomationService (suggestTitle / refinement / extract) call chain
              method(AiController.class, "suggestTitle", Note.class),
              method(
                  AiController.class,
                  "generateRefinementSuggestions",
                  Note.class,
                  NoteRefinementQuestionContextDTO.class),
              method(
                  AiController.class,
                  "removeRefinementSuggestion",
                  Note.class,
                  NoteRefinementLayoutSelectionRequestDTO.class),
              method(
                  AiController.class,
                  "extractNotePreview",
                  Note.class,
                  NoteRefinementLayoutSelectionRequestDTO.class),
              // OtherAiServices (transcription + structured response) call chain
              method(AiAudioController.class, "audioToText", AudioUploadDTO.class),
              // NoteConversationAiReplyService (streaming) call chain
              method(ConversationMessageController.class, "getAiReply", Conversation.class),
              // NoteQuestionGenerationService.generateQuestion call chain
              method(McqController.class, "generate", Note.class),
              method(McqController.class, "refine", Note.class, Mcq.class),
              method(
                  RecallPromptController.class,
                  "regenerate",
                  RecallPrompt.class,
                  QuestionContestResult.class),
              method(RecallPromptController.class, "contest", RecallPrompt.class),
              // Reaches OpenAI HTTP with no open transaction: must stay off the allowlist.
              method(MemoryTrackerController.class, "getRecallPrompt", MemoryTracker.class),
              // BookLayoutReorganizer.suggest call chain
              method(BookService.class, "suggestLayoutReorganization", Notebook.class)));

  // Candidates that are currently, knowingly, @Transactional across an OpenAI HTTP call:
  // contest/regenerate, McqController.refine, conversation/AI controllers, AiController's
  // AI-editing endpoints, and the book layout suggestion read. Shrink this list as each site
  // stops holding a transaction across the OpenAI call.
  private static final Set<Method> ALLOWLIST =
      Set.of(
          method(AiController.class, "suggestTitle", Note.class),
          method(
              AiController.class,
              "generateRefinementSuggestions",
              Note.class,
              NoteRefinementQuestionContextDTO.class),
          method(
              AiController.class,
              "removeRefinementSuggestion",
              Note.class,
              NoteRefinementLayoutSelectionRequestDTO.class),
          method(
              AiController.class,
              "extractNotePreview",
              Note.class,
              NoteRefinementLayoutSelectionRequestDTO.class),
          method(AiAudioController.class, "audioToText", AudioUploadDTO.class),
          method(ConversationMessageController.class, "getAiReply", Conversation.class),
          method(McqController.class, "refine", Note.class, Mcq.class),
          method(
              RecallPromptController.class,
              "regenerate",
              RecallPrompt.class,
              QuestionContestResult.class),
          method(RecallPromptController.class, "contest", RecallPrompt.class),
          method(BookService.class, "suggestLayoutReorganization", Notebook.class));

  @Test
  void transactionalCandidatesOnTheOpenAiHttpPathMatchTheAllowlistExactly() {
    Set<Method> actuallyTransactional =
        CANDIDATES.stream()
            .filter(m -> m.isAnnotationPresent(Transactional.class))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    assertThat(actuallyTransactional, is(ALLOWLIST));
  }
}

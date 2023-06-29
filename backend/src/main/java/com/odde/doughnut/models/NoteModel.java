package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

public class NoteModel {
  private final Note entity;
  private final ModelFactoryService modelFactoryService;

  public NoteModel(Note note, ModelFactoryService modelFactoryService) {
    this.entity = note;
    this.modelFactoryService = modelFactoryService;
  }

  public void destroy(Timestamp currentUTCTimestamp) {
    if (entity.getNotebook() != null) {
      if (entity.getNotebook().getHeadNote() == entity) {
        entity.getNotebook().setDeletedAt(currentUTCTimestamp);
        modelFactoryService.notebookRepository.save(entity.getNotebook());
      }
    }

    entity.setDeletedAt(currentUTCTimestamp);
    modelFactoryService.noteRepository.save(entity);
    modelFactoryService.noteRepository.softDeleteDescendants(entity, currentUTCTimestamp);
  }

  public void restore() {
    if (entity.getNotebook() != null) {
      if (entity.getNotebook().getHeadNote() == entity) {
        entity.getNotebook().setDeletedAt(null);
        modelFactoryService.notebookRepository.save(entity.getNotebook());
      }
    }
    modelFactoryService.noteRepository.undoDeleteDescendants(entity, entity.getDeletedAt());
    entity.setDeletedAt(null);
    modelFactoryService.noteRepository.save(entity);
  }

  public void checkDuplicateWikidataId() throws BindException {
    if (Strings.isEmpty(entity.getWikidataId())) {
      return;
    }
    List<Note> existingNotes =
        modelFactoryService.noteRepository.noteWithWikidataIdWithinNotebook(
            entity.getNotebook(), entity.getWikidataId());
    if (existingNotes.stream().anyMatch(n -> !n.equals(entity))) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(entity.getWikidataId(), "wikidataId");
      bindingResult.rejectValue(null, "error.error", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  private String getPath() {
    return entity.getAncestors().stream().map(Note::getTitle).collect(Collectors.joining(" › "));
  }

  public List<ChatMessage> getChatMessagesForGenerateQuestion() {
    List<ChatMessage> chatMessages =
        getChatMessages(
            """
        The note of current focus:
        title: %s
        description (until the end of this message)::
        %s
              """
                .formatted(entity.getTitle(), entity.getTextContent().getDescription()),
            """
        The note of current focus is something I learned in the last but is not revealed to me now.
        To help me refresh my memory about it,
        please generate a multiple-choice question with 2 to 4 options and only 1 correct option.
        Vary the option text length, so that the correct answer isn't always the longest one.
        The response should be JSON-formatted as follows:
          {
            question: "",
            options: [
              {
                option: "",
                correct: true,
              },
            ],
          }
        )}""");

    return chatMessages;
  }

  public List<ChatMessage> getChatMessagesForNoteDescriptionCompletion(
      AiSuggestionRequest aiSuggestionRequest) {
    List<ChatMessage> messages = getChatMessages(null, aiSuggestionRequest.prompt);
    if (!Strings.isEmpty(aiSuggestionRequest.incompleteAssistantMessage)) {
      messages.add(
          new ChatMessage(
              ChatMessageRole.ASSISTANT.value(), aiSuggestionRequest.incompleteAssistantMessage));
    }
    return messages;
  }

  private List<ChatMessage> getChatMessages(String additionalContext, String prompt) {
    String context = getPath();
    List<ChatMessage> messages = new ArrayList<>();
    String content =
        ("This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
                + "Current context of the note: ")
            + context;
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), content));
    if (!Strings.isBlank(additionalContext)) {
      messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), additionalContext));
    }
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
    return messages;
  }
}

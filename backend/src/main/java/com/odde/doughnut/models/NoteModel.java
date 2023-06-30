package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteMessageBuilder;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.sql.Timestamp;
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

  public String getPath() {
    return entity.getAncestors().stream().map(Note::getTitle).collect(Collectors.joining(" › "));
  }

  public List<ChatMessage> getChatMessagesForGenerateQuestion() {
    OpenAIChatAboutNoteMessageBuilder builder = new OpenAIChatAboutNoteMessageBuilder(this);
    List<ChatMessage> messages = builder.build();
    String noteOfCurrentFocus =
        """
The note of current focus:
title: %s
description (until the end of this message):
%s
      """
            .formatted(entity.getTitle(), entity.getTextContent().getDescription());
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), noteOfCurrentFocus));
    messages.add(
        new ChatMessage(
            ChatMessageRole.USER.value(),
            """
Please note that I don't know which note is of current focus.
To help me recall and refresh my memory about it,
please generate a multiple-choice question with 2 to 4 options and only 1 correct option.
Vary the option text length, so that the correct answer isn't always the longest one.
The question should be about the note of current focus in its context.
Leave the 'question' field empty if you find there's too little information to generate a question.
"""));
    return messages;
  }

  public List<ChatMessage> getChatMessagesForNoteDescriptionCompletion(
      AiSuggestionRequest aiSuggestionRequest) {
    OpenAIChatAboutNoteMessageBuilder builder = new OpenAIChatAboutNoteMessageBuilder(this);
    List<ChatMessage> messages = builder.build();
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), aiSuggestionRequest.prompt));
    if (!Strings.isEmpty(aiSuggestionRequest.incompleteAssistantMessage)) {
      messages.add(
          new ChatMessage(
              ChatMessageRole.ASSISTANT.value(), aiSuggestionRequest.incompleteAssistantMessage));
    }
    return messages;
  }
}

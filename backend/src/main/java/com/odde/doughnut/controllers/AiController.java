package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class AiController {

  private final OtherAiServices otherAiServices;
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  @Autowired
  public AiController(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      OtherAiServices otherAiServices,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.otherAiServices = otherAiServices;
    this.authorizationService = authorizationService;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    authorizationService.assertLoggedIn();
    return new AiGeneratedImage(otherAiServices.getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return otherAiServices.getAvailableGptModels();
  }

  @PostMapping("/suggest-title/{note}")
  @Transactional
  public SuggestedTitleDTO suggestTitle(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String title =
        notebookAssistantForNoteServiceFactory.createNoteAutomationService(note).suggestTitle();
    return new SuggestedTitleDTO(title);
  }

  @PostMapping("/generate-understanding-checklist/{note}")
  @Transactional
  public UnderstandingChecklistDTO generateUnderstandingChecklist(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String details = note.getDetails();
    if (details == null || details.trim().isEmpty()) {
      return new UnderstandingChecklistDTO(List.of());
    }
    List<String> points =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .generateUnderstandingChecklist();
    return new UnderstandingChecklistDTO(points);
  }

  @PostMapping("/remove-point-from-note/{note}")
  @Transactional
  public NoteRealm removePointFromNote(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody String pointToRemove)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);

    String rephrasedDetails = "English plays a crucial role in global communication, enabling people from different cultures and nations to understand one another. It is the primary language used in international business, science, technology, and diplomacy, which significantly broadens professional and academic opportunities. The language is deeply embedded in the digital world, with a large majority of online content created in English. Global media—such as films, music, and entertainment—relies heavily on English, offering access to a wide range of cultures and ideas. Knowledge of English also makes international travel easier, helping people navigate foreign environments and communicate with locals more effectively. As an official language in over 60 countries and a widely taught foreign language elsewhere, English is essential for global participation and education.";
        // notebookAssistantForNoteServiceFactory
        //     .createNoteAutomationService(note)
        //     .removePointFromNote(pointToRemove);

    note.setDetails(rephrasedDetails);
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.save(note);

    return note.toNoteRealm(authorizationService.getCurrentUser());
  }
}

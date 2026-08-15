package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.McqService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mcqs")
class McqController {
  private final McqService mcqService;

  private final AiQuestionGenerator aiQuestionGenerator;
  private final AuthorizationService authorizationService;
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final StructuredResponseCreateParamsSerializer paramsSerializer;

  @Autowired
  public McqController(
      McqService mcqService,
      AuthorizationService authorizationService,
      AiQuestionGenerator aiQuestionGenerator,
      NoteQuestionGenerationService noteQuestionGenerationService,
      StructuredResponseCreateParamsSerializer paramsSerializer) {
    this.mcqService = mcqService;
    this.authorizationService = authorizationService;
    this.aiQuestionGenerator = aiQuestionGenerator;
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.paramsSerializer = paramsSerializer;
  }

  @PostMapping("/generate")
  public Mcq generate(@RequestParam(value = "note") @Schema(type = "integer") Note note) {
    authorizationService.assertLoggedIn();
    GeneratedMcq generatedMcq = aiQuestionGenerator.getAiGeneratedQuestion(note, null);
    if (generatedMcq == null) {
      return null;
    }
    return generatedMcq.toMcq(note);
  }

  @GetMapping("/{note}")
  public List<Mcq> list(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return note.getMcqs().stream().toList();
  }

  @PostMapping("/{note}")
  @Transactional
  public Mcq add(
      @PathVariable("note") @Schema(type = "integer") Note note, @Valid @RequestBody Mcq mcq)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return mcqService.addQuestion(note, mcq);
  }

  @PostMapping("/{note}/refine")
  @Transactional
  public Mcq refine(@PathVariable("note") @Schema(type = "integer") Note note, @RequestBody Mcq mcq)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return mcqService.refineAIQuestion(note, mcq);
  }

  @GetMapping(value = "/{note}/export", produces = "application/json")
  public Map<String, Object> export(@PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    StructuredResponseCreateParams<GeneratedMcq> params =
        noteQuestionGenerationService.buildQuestionGenerationRequest(note, null);
    return paramsSerializer.toBodyMap(params);
  }
}

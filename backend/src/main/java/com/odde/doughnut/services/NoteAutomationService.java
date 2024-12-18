package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import java.util.List;

public final class NoteAutomationService {
  private final NotebookAssistantForNoteService notebookAssistantForNoteService;

  public NoteAutomationService(NotebookAssistantForNoteService notebookAssistantForNoteService) {
    this.notebookAssistantForNoteService = notebookAssistantForNoteService;
  }

  public String suggestTitle() throws JsonProcessingException {
    String instructions =
        "Please suggest a better title for the note by calling the function. Don't change it if it's already good enough.";

    AiTool tool = AiToolFactory.suggestNoteTitle();
    TitleReplacement replacement =
        notebookAssistantForNoteService
            .createThreadWithNoteInfo(List.of())
            .withTool(tool)
            .withAdditionalAdditionalInstructions(instructions)
            .run()
            .getRunResult()
            .getAssumedToolCallArgument(TitleReplacement.class);
    return replacement != null ? replacement.newTitle : null;
  }
}

package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolName;

public class DummyForGeneratingTypes {
  // Note: Assistants API types (Message, RunStep, Run, MessageDelta, DeltaOfRunStep) were removed
  // as they are not supported by the official OpenAI Java SDK and are not used in the codebase.
  // The official SDK only supports Chat Completions API, not the Assistants API.
  public NoteDetailsCompletion noteDetailsCompletion;
  public TitleReplacement titleReplacement;
  public AiToolName aiToolName;
}

package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.content.MessageDelta;
import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.assistants.run_step.RunStep;
import com.theokanning.openai.assistants.run_step.StepDetails;

public class DummyForGeneratingTypes {
  public Message message;
  public RunStep runStep;
  public DeltaOfRunStep runStepDelta;
  public MessageDelta messageDelta;
  public Run run;
  public NoteDetailsCompletion noteDetailsCompletion;

  // The open api library we use messed up the class name Delta, so we have to use a different name
  // here.
  public static class DeltaOfRunStep {
    /** The details of the run step. */
    @JsonProperty("delta")
    StepDelta delta;
  }

  public static class StepDelta {
    /** The details of the run step. */
    @JsonProperty("step_details")
    StepDetails stepDetails;
  }
}

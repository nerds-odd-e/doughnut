package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.theokanning.openai.assistants.message.content.MessageDelta;
import com.theokanning.openai.assistants.run.*;

public class DummyForGeneratingTypes {
  public MessageDelta messageDelta;
  public Run run;
  public NoteDetailsCompletion noteDetailsCompletion;
}

package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.RepetitionForUser;
import org.springframework.lang.Nullable;

public class AnswerResult {
  public Integer answerId;
  public boolean correct;
  @Nullable public RepetitionForUser nextRepetition;
}

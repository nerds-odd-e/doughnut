package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class MarkedQuestionRequest {
  public Integer noteId;
  public Integer quizQuestionId;
  public String comment;
  public Boolean isGood;

}

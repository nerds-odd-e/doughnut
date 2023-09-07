package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MarkedQuestionRequest {
  public Integer noteId;
  public Integer quizQuestionId;
  public String comment;
  public Boolean isGood;
}

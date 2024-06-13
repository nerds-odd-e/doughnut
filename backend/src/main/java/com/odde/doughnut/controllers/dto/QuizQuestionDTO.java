package com.odde.doughnut.entities;

import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.sql.Timestamp;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class QuizQuestionDTO {

  @Getter @Setter private MultipleChoicesQuestion multipleChoicesQuestion;

  @Getter @Setter private Timestamp createdAt;

  @Getter @Setter private Integer correctAnswerIndex;
}

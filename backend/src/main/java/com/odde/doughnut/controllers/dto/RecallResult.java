package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.QuestionType;

public record RecallResult(AnsweredQuestion answeredQuestion, QuestionType questionType) {}

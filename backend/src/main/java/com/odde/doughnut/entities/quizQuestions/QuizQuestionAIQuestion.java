package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("11")
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "imageWithMask"})
public class QuizQuestionAIQuestion extends QuizQuestionEntity {}

package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.*;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public abstract class QuizQuestionWithNoteChoices extends QuizQuestionEntity {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id")
  @Getter
  @Setter
  private LinkingNote categoryLink;
}

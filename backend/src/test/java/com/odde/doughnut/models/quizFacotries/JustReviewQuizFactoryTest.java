package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.JUST_REVIEW;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class JustReviewQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note note;
  Link link;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().byUser(userModel).please();
    Note anotherNote = makeMe.aNote().byUser(userModel).please();
    link = makeMe.aLink().between(note, anotherNote).please();
    makeMe.refresh(note);
  }

  @Test
  void shouldWorkForNote() {
    ReviewPoint reviewPoint = makeMe.aReviewPointFor(note).by(userModel).please();
    QuizQuestionViewedByUser quizQuestion = buildQuestion(reviewPoint);
    assertThat(quizQuestion.getQuestionType(), equalTo(JUST_REVIEW));
  }

  @Test
  void shouldWorkForLink() {
    ReviewPoint reviewPoint = makeMe.aReviewPointFor(link).by(userModel).please();
    QuizQuestionViewedByUser quizQuestion = buildQuestion(reviewPoint);
    assertThat(quizQuestion.getQuestionType(), equalTo(JUST_REVIEW));
  }

  private QuizQuestionViewedByUser buildQuestion(ReviewPoint reviewPoint) {
    return new QuizQuestionViewedByUser(buildQuizQuestion(reviewPoint), makeMe.modelFactoryService);
  }

  private QuizQuestion buildQuizQuestion(ReviewPoint reviewPoint) {
    return reviewPoint.createAQuizQuestionOfType(QuestionType.JUST_REVIEW);
  }
}

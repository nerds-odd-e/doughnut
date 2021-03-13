package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    @Test
    void aNoteWithNoDescriptionHasNoQuiz() {
        NoteEntity noteEntity = makeMe.aNote().withNoDescription().byUser(userModel).please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion, is(nullValue()));
    }

    @Test
    void aNoteWithDescriptionHasAutoClozeDeletionQuiz() {
        NoteEntity noteEntity = makeMe.aNote().please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion.getDescription(), equalTo(noteEntity.getDescription()));
    }

    @Test
    void whenDescripitonIncludesTitle() {
        NoteEntity noteEntity = makeMe.aNote().title("Sedition").description("Word sedition means incite violence").please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion.getDescription(), equalTo("Word [...] means incite violence"));
    }

    @Test
    void aNoteWithNoSiblings() {
        NoteEntity noteEntity = makeMe.aNote().please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion.getOptions(), contains(noteEntity.getTitle()));
    }

    @Test
    void aNoteWith1Siblings() {
        NoteEntity top = makeMe.aNote().please();
        NoteEntity noteEntity1 = makeMe.aNote().under(top).please();
        NoteEntity noteEntity2 = makeMe.aNote().under(top).please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity1);
        assertThat(quizQuestion.getOptions(), containsInAnyOrder(noteEntity1.getTitle(), noteEntity2.getTitle()));
    }

    private QuizQuestion getQuizQuestion(NoteEntity noteEntity) {
        ReviewPointModel reviewPoint = makeMe.aReviewPointFor(noteEntity).by(userModel).toModelPlease();
        return reviewPoint.generateAQuizQuestion();
    }

}
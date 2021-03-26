package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTypesTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    @Nested
    class LinkSourceExclusive {
        @Test
        void shouldIncluveRightAnswers() {
            NoteEntity note1 = makeMe.aNote().please();
            NoteEntity note2 = makeMe.aNote().byUser(userModel.getEntity()).linkTo(note1).please();
            ReviewPointEntity reviewPointEntity = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
            QuizQuestionBuilder builder = new QuizQuestionBuilder(LINK_SOURCE_EXCLUSIVE, randomizer, reviewPointEntity, makeMe.modelFactoryService);
            QuizQuestion quizQuestion = builder.buildQuizQuestion();
            assertThat(quizQuestion.getDescription(), equalTo("Which of the following does not belong to"));
            assertThat(quizQuestion.getMainTopic(), equalTo(note1.getTitle()));
        }
    }
}


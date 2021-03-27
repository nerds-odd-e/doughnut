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
import static org.hamcrest.Matchers.*;

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
        NoteEntity top;
        NoteEntity target;
        NoteEntity source;
        NoteEntity anotherSource;
        ReviewPointEntity reviewPointEntity;

        @BeforeEach
        void setup() {
            top = makeMe.aNote().please();
            target = makeMe.aNote("target").under(top).please();
            source = makeMe.aNote("source").under(top).byUser(userModel.getEntity()).linkTo(target).please();
            anotherSource = makeMe.aNote("anotherSource").under(top).byUser(userModel.getEntity()).linkTo(target).please();
            reviewPointEntity = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        }

        @Test
        void shouldIncludeRightAnswers() {
            NoteEntity notRelated = makeMe.aNote("noteRelated").under(top).please();
            QuizQuestionFactory builder = new QuizQuestionFactory(LINK_SOURCE_EXCLUSIVE, randomizer, reviewPointEntity, makeMe.modelFactoryService);
            QuizQuestion quizQuestion = builder.buildQuizQuestion();
            assertThat(quizQuestion.getDescription(), equalTo("Which of the following does not belong to"));
            assertThat(quizQuestion.getMainTopic(), equalTo(target.getTitle()));
            List<String> options = quizQuestion.getOptions().stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
            assertThat(anotherSource.getTitle(), in(options));
            assertThat(notRelated.getTitle(), in(options));
            assertThat(source.getTitle(), in(options));
        }
    }
}


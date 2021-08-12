package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
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

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class DescriptionLinkTargetQuizFactoryTest {

    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();
    Note top;
    Note target;
    Note source;
    Note anotherSource;
    ReviewPoint reviewPoint;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        top = makeMe.aNote().byUser(userModel).please();
        target = makeMe.aNote("rome").under(top).please();
        source = makeMe.aNote("saying").description("Rome is not built in a day").under(top).linkTo(target).please();
        reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).by(userModel).inMemoryPlease();
        anotherSource = makeMe.aNote("pompeii").under(top).please();
        makeMe.refresh(top);
    }

    @Nested
    class WhenTheNoteDoesnotHaveDescription {
        @BeforeEach
        void setup() {
            makeMe.theNote(source).description("").please();
        }

        @Test
        void shouldBeInvalid() {
            QuizQuestion quizQuestion = buildQuestion();
            assertThat(quizQuestion, nullValue());
        }
    }

    @Test
    void shouldIncludeRightAnswers() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion.getDescription(), containsString("<p>The following descriptions is a specialization of:</p><pre><mark title='Hidden text that is matching the answer'>[...]</mark> is not built in a day</pre>"));
    }

    @Nested
    class WhenThereIsReviewPointForTheSourceNote {
        ReviewPoint sourceReviewPoint;
        @BeforeEach
        void setup() {
            sourceReviewPoint = makeMe.aReviewPointFor(source).by(userModel).please();
            makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswers() {
            QuizQuestion quizQuestion = buildQuestion();
            assertThat(quizQuestion.getViceReviewPointIds(), contains(sourceReviewPoint.getId()));
        }

    }

    @Nested
    class Answer {
        @Test
        void correct() {
            AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(DESCRIPTION_LINK_TARGET)
                    .answer(target.getTitle())
                    .inMemoryPlease();
            assertTrue(answerResult.isCorrect());
        }
    }



    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(DESCRIPTION_LINK_TARGET, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

}


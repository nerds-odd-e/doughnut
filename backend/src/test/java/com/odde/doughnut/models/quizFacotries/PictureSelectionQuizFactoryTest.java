package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
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

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.PICTURE_SELECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class PictureSelectionQuizFactoryTest {
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
        source = makeMe.aNote("source").under(top).please();
        anotherSource = makeMe.aNote("another note").under(top).please();
        reviewPoint = makeMe.aReviewPointFor(source).inMemoryPlease();
        makeMe.refresh(top);
    }

    @Test
    void shouldReturnNullIfCannotFindPicture() {
        QuizQuestion quizQuestion = buildLinkTargetQuizQuestion();
        assertThat(quizQuestion, is(nullValue()));
    }

    @Nested
    class WhenThereIsPicture {
        @BeforeEach
        void setup() {
            makeMe.theNote(source).pictureUrl("http://img/img.jpg").please();
        }

        @Test
        void shouldIncludeRightAnswers() {
            QuizQuestion quizQuestion = buildLinkTargetQuizQuestion();
            assertThat(quizQuestion.getDescription(), equalTo(""));
            assertThat(quizQuestion.getMainTopic(), equalTo("source"));
            List<String> options = toOptionStrings(quizQuestion);
            assertThat(source.getTitle(), in(options));
        }

        @Nested
        class Answer {
            @Test
            void correct() {
                AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                        .type(PICTURE_SELECTION)
                        .answer(source.getTitle())
                        .inMemoryPlease();
                assertTrue(answerResult.isCorrect());
            }
        }



    }

    private QuizQuestion buildLinkTargetQuizQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(PICTURE_SELECTION, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        return quizQuestion.getOptions().stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}


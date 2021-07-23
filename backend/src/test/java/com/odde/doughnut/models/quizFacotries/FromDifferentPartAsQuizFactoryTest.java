package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
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

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_DIFFERENT_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_SAME_PART_AS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class FromDifferentPartAsQuizFactoryTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();
    Note top;
    Note perspective;
    Note subjective;
    Note objective;
    Note ugly;
    Note pretty;
    Note tall;
    Note kind;
    Link subjectivePerspective;
    ReviewPoint reviewPoint;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        top = makeMe.aNote("top").byUser(userModel).please();
        perspective = makeMe.aNote("perspective").under(top).please();
        subjective = makeMe.aNote("subjective").under(top).please();
        objective = makeMe.aNote("objective").under(top).please();
        ugly = makeMe.aNote("ugly").under(top).please();
        pretty = makeMe.aNote("pretty").under(top).please();
        kind = makeMe.aNote("kind").under(top).please();
        tall = makeMe.aNote("tall").under(top).please();
        subjectivePerspective = makeMe.aLink().between(subjective, perspective, Link.LinkType.PART).please();
        makeMe.aLink().between(objective, perspective, Link.LinkType.PART).please();
        makeMe.aLink().between(kind, subjective, Link.LinkType.TAGGED_BY).please();
        Link uglySubjective = makeMe.aLink().between(ugly, subjective, Link.LinkType.TAGGED_BY).please();
        reviewPoint = makeMe.aReviewPointFor(uglySubjective).by(userModel).inMemoryPlease();
        makeMe.refresh(top);
    }

    @Test
    void shouldBeInvalidWhenNoCousin() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion, nullValue());
    }

    @Nested
    class WhenThereIsAnCousin {
        Link cousin;

        @BeforeEach
        void setup() {
            cousin = makeMe.aLink().between(pretty, subjective, Link.LinkType.TAGGED_BY).please();
            makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldBeInvalidWhenNoFillingOptions() {
            QuizQuestion quizQuestion = buildQuestion();
            assertThat(quizQuestion, nullValue());
        }

        @Nested
        class WhenThereIsFillingOption {

            @BeforeEach
            void setup() {
                makeMe.aLink().between(tall, objective, Link.LinkType.TAGGED_BY).please();
                makeMe.refresh(userModel.getEntity());
            }

            @Test
            void shouldIncludeRightAnswersAndFillingOptions() {
                QuizQuestion quizQuestion = buildQuestion();
                assertThat(quizQuestion.getDescription(), containsString("<p>Which one's <mark>perspective</mark> is <em>different</em> from:"));
                assertThat(quizQuestion.getMainTopic(), containsString(ugly.getTitle()));
                List<String> strings = toOptionStrings(quizQuestion);
                assertThat(pretty.getTitle(), in(strings));
                assertThat(tall.getTitle(), in(strings));
                assertThat(kind.getTitle(), in(strings));
                assertThat(ugly.getTitle(), not(in(strings)));
            }

            @Nested
            class WhenThereIsReviewPointOfTheCategory {
                ReviewPoint additionalReviewPoint;

                @BeforeEach
                void setup() {
                    additionalReviewPoint = makeMe.aReviewPointFor(subjectivePerspective).by(userModel).please();
                }

                @Test
                void shouldInclude2ViceReviewPoints() {
                    QuizQuestion quizQuestion = buildQuestion();
                    List<Integer> viceReviewPointIds = quizQuestion.getViceReviewPointIds();
                    assertThat(additionalReviewPoint.getId(), in(viceReviewPointIds));
                }

            }

            @Nested
            class Answer {
                @Test
                void correct() {
                    AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                            .type(FROM_DIFFERENT_PART_AS)
                            .answer(pretty.getTitle())
                            .inMemoryPlease();
                    assertFalse(answerResult.isCorrect());
                }

                @Test
                void wrong() {
                    AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                            .type(FROM_DIFFERENT_PART_AS)
                            .answer("metal")
                            .inMemoryPlease();
                    assertTrue(answerResult.isCorrect());
                }
            }
        }

    }

    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(FROM_DIFFERENT_PART_AS, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        List<QuizQuestion.Option> options = quizQuestion.getOptions();
        return options.stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}


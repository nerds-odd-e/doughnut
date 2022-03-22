package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class WhichSpecHasInstanceQuizFactoryTest {
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
        top = makeMe.aNote("top").byUser(userModel).please();
        target = makeMe.aNote("element").under(top).please();
        source = makeMe.aNote("noble gas").under(top).linkTo(target, Link.LinkType.SPECIALIZE).please();
        anotherSource = makeMe.aNote("non-official name").under(top).please();
        reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).by(userModel).inMemoryPlease();
        makeMe.refresh(top);
    }

    @Test
    void shouldBeInvalidWhenNoInsatnceOfLink() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion, nullValue());
    }

    @Nested
    class WhenTheNoteHasInstance {
        @BeforeEach
        void setup() {
            makeMe.theNote(source).linkTo(anotherSource, Link.LinkType.INSTANCE);
        }

        @Test
        void shouldBeInvalidWhenNoInsatnceOfLink() {
            QuizQuestion quizQuestion = buildQuestion();
            assertThat(quizQuestion, nullValue());
        }

        @Nested
        class WhenTheNoteHasMoreSpecificationSiblings {
            Note metal;

            @BeforeEach
            void setup() {
                metal = makeMe.aNote("metal").under(top).linkTo(target, Link.LinkType.SPECIALIZE).please();
            }

            @Test
            void shouldBeInvalidWhenNoViceReviewPoint() {
                QuizQuestion quizQuestion = buildQuestion();
                assertThat(quizQuestion, nullValue());
            }

            @Nested
            class WhenTheSecondLinkHasReviewPoint {

                @BeforeEach
                void setup() {
                    makeMe.aReviewPointFor(source.getLinks().get(1)).by(userModel).please();
                    makeMe.refresh(userModel.getEntity());
                }

                @Test
                void shouldIncludeRightAnswers() {
                    QuizQuestion quizQuestion = buildQuestion();
                    assertThat(quizQuestion.getDescription(), containsString("<p>Which one is a specialization of <mark>element</mark> <em>and</em> is an instance of <mark>non-official name</mark>:"));
                    List<String> strings = toOptionStrings(quizQuestion);
                    assertThat("metal", in(strings));
                    assertThat(source.getTitle(), in(strings));
                }

                @Nested
                class Answer {
                    @Test
                    void correct() {
                        AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                                .type(WHICH_SPEC_HAS_INSTANCE)
                                .answer(source.getTitle())
                                .inMemoryPlease();
                        assertTrue(answerResult.isCorrect());
                    }

                    @Test
                    void wrong() {
                        AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                                .type(WHICH_SPEC_HAS_INSTANCE)
                                .answer("metal")
                                .inMemoryPlease();
                        assertFalse(answerResult.isCorrect());
                    }
                }

                @Nested
                class PersonAlsoHasTheSameNoteAsInstance {

                    @BeforeEach
                    void setup() {
                        makeMe.theNote(metal).linkTo(anotherSource, Link.LinkType.INSTANCE).please();
                    }

                    @Test
                    void shouldBeInvalid() {
                        QuizQuestion quizQuestion = buildQuestion();
                        assertThat(quizQuestion, nullValue());
                    }
                }

                @Nested
                class OptionFromInstance {

                    @BeforeEach
                    void setup() {
                        makeMe.aNote("something else").under(top).linkTo(anotherSource, Link.LinkType.INSTANCE).please();
                        makeMe.refresh(top);
                    }

                    @Test
                    void options() {
                        QuizQuestion quizQuestion = buildQuestion();
                        List<String> strings = toOptionStrings(quizQuestion);
                        assertThat("something else", in(strings));
                    }
                }
            }

        }

    }

    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(WHICH_SPEC_HAS_INSTANCE, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        List<QuizQuestionViewedByUser.Option> options = quizQuestion.getOptions();
        return options.stream().map(QuizQuestionViewedByUser.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}


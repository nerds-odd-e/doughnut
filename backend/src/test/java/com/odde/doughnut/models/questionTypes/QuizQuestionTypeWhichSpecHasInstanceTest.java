package com.odde.doughnut.models.questionTypes;

import com.odde.doughnut.entities.Link;
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

import java.util.List;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTypeWhichSpecHasInstanceTest {
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
        reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
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
            void shouldIncludeRightAnswers() {
                QuizQuestion quizQuestion = buildQuestion();
                assertThat(quizQuestion.getDescription(), containsString("<p>Which one is a specialization of <mark>element</mark> <em>and</em> is an instance of <mark>non-official name</mark>:"));
                List<String> strings = toOptionStrings(quizQuestion);
                assertThat("metal", in(strings));
                assertThat(source.getTitle(), in(strings));
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
                    makeMe.aNote("something else").under(top).linkTo(target, Link.LinkType.INSTANCE).please();
                }

                @Test
                void options() {
                    QuizQuestion quizQuestion = buildQuestion();
                    List<String> strings = toOptionStrings(quizQuestion);
//                    assertThat("something else", in(strings));
                }
            }

        }

    }

    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(WHICH_SPEC_HAS_INSTANCE, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        List<QuizQuestion.Option> options = quizQuestion.getOptions();
        return options.stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}


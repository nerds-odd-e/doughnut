package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

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
class LinkTargetExclusiveQuizFactoryTest {
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
        Note top;
        Note target;
        Note source;
        Note anotherSource;
        ReviewPoint reviewPoint;

        @BeforeEach
        void setup() {
            top = makeMe.aNote().please();
            target = makeMe.aNote("target").under(top).please();
            source = makeMe.aNote("source").under(top).byUser(userModel.getEntity()).linkTo(target).please();
            reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        }

        @Test
        void shouldReturnNullIfCannotFindCandidateAnswer() {
            QuizQuestion quizQuestion = buildLinSourceExclusiveQuizQuestion();
            assertThat(quizQuestion, is(nullValue()));
        }

        @Nested
        class WithValidExclusiveAnswer {
            Note notRelated;

            @BeforeEach
            void setup() {
                notRelated = makeMe.aNote("noteRelated").under(top).please();
            }

            @Test
            void shouldReturnNullIfNoEnoughOptions() {
                QuizQuestion quizQuestion = buildLinSourceExclusiveQuizQuestion();
                assertThat(quizQuestion, is(nullValue()));
            }

            @Nested
            class WithEnoughOptions {
                @BeforeEach
                void setup() {
                    anotherSource = makeMe.aNote("anotherSource").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                }


                @Test
                void shouldIncludeRightAnswers() {
                    makeMe.refresh(top);
                    QuizQuestion quizQuestion = buildLinSourceExclusiveQuizQuestion();
                    assertThat(quizQuestion.getDescription(), equalTo("Which of the following is <em>NOT</em> a specialization of"));
                    assertThat(quizQuestion.getMainTopic(), equalTo(target.getTitle()));
                    List<String> options = toOptionStrings(quizQuestion);
                    assertThat(anotherSource.getTitle(), in(options));
                    assertThat(source.getTitle(), in(options));
                    assertThat(notRelated.getTitle(), in(options));
                }

                @Test
                void mustIncludeSourceNote() {
                    makeMe.aNote("anotherSource1").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    makeMe.aNote("anotherSource2").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    makeMe.aNote("anotherSource3").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    makeMe.aNote("anotherSource4").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    makeMe.aNote("anotherSource5").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    source = makeMe.aNote("anotherSource6").under(top).byUser(userModel.getEntity()).linkTo(target).please();
                    makeMe.refresh(top);
                    reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
                    QuizQuestion quizQuestion = buildLinSourceExclusiveQuizQuestion();
                    List<String> options = toOptionStrings(quizQuestion);
                    assertThat(notRelated.getTitle(), in(options));
                    assertThat(source.getTitle(), in(options));
                }
            }
        }

        private QuizQuestion buildLinSourceExclusiveQuizQuestion() {
            QuizQuestionDirector builder = new QuizQuestionDirector(LINK_SOURCE_EXCLUSIVE, randomizer, reviewPoint, makeMe.modelFactoryService);
            return builder.buildQuizQuestion();
        }
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        return quizQuestion.getOptions().stream().map(QuizQuestionViewedByUser.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}


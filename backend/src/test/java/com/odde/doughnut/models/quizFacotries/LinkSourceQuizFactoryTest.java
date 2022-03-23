package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Note;
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

import java.util.List;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class LinkSourceQuizFactoryTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    Note top;
    Note target;
    Note source;
    Note anotherSource;
    ReviewPoint reviewPoint;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        top = makeMe.aNote().byUser(userModel).please();
        target = makeMe.aNote("target").under(top).please();
        source = makeMe.aNote("source").under(top).linkTo(target).please();
        anotherSource = makeMe.aNote("another note").under(top).please();
        reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        makeMe.refresh(top);
    }

    @Test
    void shouldReturnNullIfCannotFindEnoughOptions() {
        makeMe.aLink().between(anotherSource, target).please();
        makeMe.refresh(top);
        assertThat( buildLinkTargetQuizQuestion(), is(nullValue()));
    }

    @Nested
    class WhenThereAreMoreThanOneOptions {
        @BeforeEach
        void setup() {
            makeMe.refresh(top);
        }

        @Test
        void shouldIncludeRightAnswers() {
            QuizQuestionViewedByUser quizQuestion = buildLinkTargetQuizQuestion();
            assertThat(quizQuestion.getDescription(), equalTo("Which one <em>is immediately a specialization of</em>:"));
            assertThat(quizQuestion.getMainTopic(), equalTo("target"));
            List<String> options = toOptionStrings(quizQuestion);
            assertThat(anotherSource.getTitle(), in(options));
            assertThat(source.getTitle(), in(options));
        }

        @Nested
        class Answer {
            @Test
            void correct() {
                AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                        .type(LINK_SOURCE)
                        .answer(source.getTitle())
                        .inMemoryPlease();
                assertTrue(answerResult.isCorrect());
            }
        }



    }

    private QuizQuestionViewedByUser buildLinkTargetQuizQuestion() {
        return makeMe.buildAQuestion(LINK_SOURCE, reviewPoint);
    }

    private List<String> toOptionStrings(QuizQuestionViewedByUser quizQuestion) {
        return quizQuestion.getOptions().stream().map(QuizQuestionViewedByUser.Option::getDisplay).toList();
    }
}


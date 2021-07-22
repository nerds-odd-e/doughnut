package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
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
import java.util.Map;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTypesClozeSelectionTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    @Nested
    class ClozeQuestion {
        Note top;
        Note note1;
        Note note2;
        ReviewPoint reviewPoint;

        @BeforeEach
        void setup() {
            top = makeMe.aNote().please();
            note1 = makeMe.aNote("target").under(top).byUser(userModel).please();
            note2 = makeMe.aNote("source").under(top).byUser(userModel).please();
            reviewPoint = makeMe.aReviewPointFor(note1).inMemoryPlease();
        }

        @Test
        void shouldIncludeRightAnswers() {
            makeMe.refresh(top);
            QuizQuestion quizQuestion = buildClozeQuizQuestion();
            assertThat(quizQuestion.getDescription(), equalTo("descrption"));
            assertThat(quizQuestion.getMainTopic(), equalTo(""));
            List<String> options = toOptionStrings(quizQuestion);
            assertThat(note2.getTitle(), in(options));
            assertThat(note1.getTitle(), in(options));
        }

        @Test
        void shouldIncludeOpenLinks() {
            makeMe.theNote(note1).linkTo(note2, Link.LinkType.TAGGED_BY).please();
            makeMe.theNote(note1).linkTo(note2, Link.LinkType.SPECIALIZE).please();
            makeMe.refresh(top);
            QuizQuestion quizQuestion = buildClozeQuizQuestion();
            Map<Link.LinkType, LinkViewed> hintLinks = quizQuestion.getHintLinks();
            assertThat(Link.LinkType.TAGGED_BY, in(hintLinks.keySet()));
            assertThat(Link.LinkType.SPECIALIZE, not(in(hintLinks.keySet())));
        }

        private QuizQuestion buildClozeQuizQuestion() {
            QuizQuestionDirector builder = new QuizQuestionDirector(CLOZE_SELECTION, randomizer, reviewPoint, makeMe.modelFactoryService);
            return builder.buildQuizQuestion();
        }

        private List<String> toOptionStrings(QuizQuestion quizQuestion) {
            return quizQuestion.getOptions().stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
        }
    }
}


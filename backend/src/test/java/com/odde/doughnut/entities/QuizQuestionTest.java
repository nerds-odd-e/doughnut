package com.odde.doughnut.entities;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    @Test
    void aNoteWithNoDescriptionHasNoQuiz() {
        Note note = makeMe.aNote().withNoDescription().byUser(userModel).please();
        QuizQuestion quizQuestion = getQuizQuestion(note);
        assertThat(quizQuestion, is(nullValue()));
    }

    @Test
    void useClozeDescription() {
        Note note = makeMe.aNote().title("abc").description("abc has 3 letters").please();
        QuizQuestion quizQuestion = getQuizQuestion(note);
        assertThat(quizQuestion.getDescription(), equalTo("<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
    }

    @Nested
    class ClozeSelectionQuiz {
        private List<String> getOptions(Note note) {
            QuizQuestion quizQuestion = getQuizQuestion(note);
            List<String> options = quizQuestion.getOptions().stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
            return options;
        }

        @Test
        void aNoteWithNoSiblings() {
            Note note = makeMe.aNote().please();
            List<String> options = getOptions(note);
            assertThat(options, contains(note.getTitle()));
        }

        @Test
        void aNoteWithOneSibling() {
            Note top = makeMe.aNote().please();
            Note note1 = makeMe.aNote().under(top).please();
            Note note2 = makeMe.aNote().under(top).please();
            makeMe.refresh(top);
            List<String> options = getOptions(note1);
            assertThat(options, containsInAnyOrder(note1.getTitle(), note2.getTitle()));
        }

        @Test
        void aNoteWithManySiblings() {
            Note top = makeMe.aNote().please();
            makeMe.theNote(top).with10Children().please();
            Note note = makeMe.aNote().under(top).please();
            makeMe.refresh(top);
            List<String> options = getOptions(note);
            assertThat(options.size(), equalTo(6));
            assertThat(options.contains(note.getTitle()), is(true));
        }
    }

    @Nested
    class SpellingQuiz {
        Note note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote().rememberSpelling().please();
        }

        @Test
        void typeShouldBeSpellingQuiz() {
            assertThat(getQuizQuestion(note).getQuestionType(), equalTo(QuizQuestion.QuestionType.SPELLING));
        }

        @Test
        void shouldReturnTheSameType() {
            ReviewPointModel reviewPoint = getReviewPointModel(note);
            QuizQuestion randomQuizQuestion = reviewPoint.generateAQuizQuestion(new RealRandomizer());
            Set<QuizQuestion.QuestionType> types = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                types.add(randomQuizQuestion.getQuestionType());
            }
            assertThat(types, hasSize(1));
        }

        @Test
        void shouldChooseTypeRandomly() {
            Set<QuizQuestion.QuestionType> types = new HashSet<>();
            ReviewPointModel reviewPoint = getReviewPointModel(note);
            for (int i = 0; i < 10; i++) {
                QuizQuestion randomQuizQuestion = reviewPoint.generateAQuizQuestion(new RealRandomizer());
                types.add(randomQuizQuestion.getQuestionType());
            }
            assertThat(types, containsInAnyOrder(QuizQuestion.QuestionType.SPELLING, QuizQuestion.QuestionType.CLOZE_SELECTION));
        }

    }

    private QuizQuestion getQuizQuestion(Note note) {
        return getReviewPointModel(note).generateAQuizQuestion(randomizer);
    }

    private ReviewPointModel getReviewPointModel(Note note) {
        return makeMe.aReviewPointFor(note).by(userModel).toModelPlease();
    }

}
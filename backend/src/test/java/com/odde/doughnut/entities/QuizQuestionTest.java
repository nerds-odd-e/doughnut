package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
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
        assertThat( getQuizQuestion(note).getQuestionType(), equalTo(QuizQuestion.QuestionType.JUST_REVIEW));
    }

    @Test
    void useClozeDescription() {
        Note note = makeMe.aNote().title("abc").description("abc has 3 letters").please();
        QuizQuestionViewedByUser quizQuestion = getQuizQuestion(note);
        assertThat(quizQuestion.getDescription(), equalTo("<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
    }

    @Nested
    class ClozeSelectionQuiz {
        private List<String> getOptions(Note note) {
            QuizQuestionViewedByUser quizQuestion = getQuizQuestion(note);
            return quizQuestion.getOptions().stream().map(QuizQuestionViewedByUser.Option::getDisplay).toList();
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
            assertThat(options.size(), equalTo(4));
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

    private QuizQuestionViewedByUser getQuizQuestion(Note note) {
        return QuizQuestionViewedByUser.from(getReviewPointModel(note).generateAQuizQuestion(randomizer), makeMe.modelFactoryService.noteRepository);
    }

    private ReviewPointModel getReviewPointModel(Note note) {
        return makeMe.aReviewPointFor(note).by(userModel).toModelPlease();
    }

}
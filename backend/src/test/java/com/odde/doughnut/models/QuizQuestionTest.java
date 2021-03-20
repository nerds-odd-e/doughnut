package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        NoteEntity noteEntity = makeMe.aNote().withNoDescription().byUser(userModel).please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion, is(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
            "moon,            partner of earth,                    partner of earth",
            "Sedition,        word sedition means this,            word [...] means this",
            "north / up,      it's on the north or up side,        it's on the [...] or [...] side",
            "cats,            a cat,                               a [..~]",
            "cat-dog,         cat dog,                             [...]",
            "cat dog,         cat-dog,                             [...]",
            "cat dog,         cat and dog,                         [...]",
            "cat dog,         cat a dog,                           [...]",
            "cat dog,         cat the dog,                         [...]",
            "cat the dog,     cat dog,                             [...]",
    })
    void clozeDescription(String title, String description, String expectedClozeDescription) {
        NoteEntity noteEntity = makeMe.aNote().title(title).description(description).please();
        QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
        assertThat(quizQuestion.getClozeDescription(), equalTo(expectedClozeDescription));
    }

    @Nested
    class ClozeSelectionQuiz {

        @Test
        void aNoteWithNoSiblings() {
            NoteEntity noteEntity = makeMe.aNote().please();
            QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
            assertThat(quizQuestion.getOptions(), contains(noteEntity.getTitle()));
        }

        @Test
        void aNoteWithOneSibling() {
            NoteEntity top = makeMe.aNote().please();
            NoteEntity noteEntity1 = makeMe.aNote().under(top).please();
            NoteEntity noteEntity2 = makeMe.aNote().under(top).please();
            QuizQuestion quizQuestion = getQuizQuestion(noteEntity1);
            assertThat(quizQuestion.getOptions(), containsInAnyOrder(noteEntity1.getTitle(), noteEntity2.getTitle()));
        }

        @Test
        void aNoteWithManySiblings() {
            NoteEntity top = makeMe.aNote().please();
            makeMe.theNote(top).with10Children().please();
            NoteEntity noteEntity = makeMe.aNote().under(top).please();
            QuizQuestion quizQuestion = getQuizQuestion(noteEntity);
            assertThat(quizQuestion.getOptions().size(), equalTo(6));
            assertThat(quizQuestion.getOptions().contains(noteEntity.getTitle()), is(true));
        }
    }

    @Nested
    class SpellingQuiz {
        NoteEntity note;

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

    private QuizQuestion getQuizQuestion(NoteEntity noteEntity) {
        ReviewPointModel reviewPoint = getReviewPointModel(noteEntity);
        return reviewPoint.generateAQuizQuestion(randomizer);
    }

    private ReviewPointModel getReviewPointModel(NoteEntity noteEntity) {
        return makeMe.aReviewPointFor(noteEntity).by(userModel).toModelPlease();
    }

}
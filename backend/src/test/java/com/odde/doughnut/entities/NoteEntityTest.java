package com.odde.doughnut.entities;

import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.NoteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

import static com.odde.doughnut.entities.LinkEntity.LinkType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional

public class NoteEntityTest {

    @Autowired MakeMe makeMe;
    UserEntity userEntity;

    @Test
    void timeOrder() {
        NoteEntity note1 = makeMe.aNote().inMemoryPlease();
        NoteEntity note2 = makeMe.aNote().inMemoryPlease();
        assertThat(note1.getSiblingOrder(), is(lessThan(note2.getSiblingOrder())));
    }

    @Nested
    class ValidationTest {
        private Validator validator;
        private final NoteEntity note = makeMe.aNote().inMemoryPlease();

        @BeforeEach
        public void setUp() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        public void defaultNoteFromMakeMeIsValidate() {
            assertThat(getViolations(), is(empty()));
        }

        @Test
        public void titleIsNotOptional() {
            note.setTitle("");
            assertThat(getViolations(), is(not(empty())));
        }

        @Test
        public void titleCannotBeTooLong() {
            note.setTitle(makeMe.aStringOfLength(101));
            assertThat(getViolations(), is(not(empty())));
        }

        private Set<ConstraintViolation<NoteEntity>> getViolations() {
            return validator.validate(note);
        }

    }

    @Nested
    class NoteEntityWithUserEntity {
        @Autowired private ModelFactoryService modelFactoryService;

        @BeforeEach
        void setup() {
            userEntity = makeMe.aUser().with2Notes().please();
        }

        @Test
        void thereShouldBe2NodesForUser() {
            List<NoteEntity> notes = userEntity.getNotes();
            assertThat(notes, hasSize(equalTo(2)));
        }

        @Test
        void targetIsEmptyByDefault() {
            NoteEntity note = userEntity.getNotes().get(0);
            assertThat(note.getTargetNotes(), is(empty()));
        }

        @Test
        void targetOfLinkedNotes() {
            NoteEntity note = userEntity.getNotes().get(0);
            NoteEntity targetNote = userEntity.getNotes().get(1);
            makeMe.theNote(note).linkTo(targetNote).please();
            List<NoteEntity> targetNotes = note.getTargetNotes();
            assertThat(targetNotes, hasSize(equalTo(1)));
            assertThat(targetNotes, contains(targetNote));
        }
    }

    @Nested
    class LinkTypes {
        NoteEntity noteEntityA;
        NoteEntity noteEntityB;

        @BeforeEach
        void setup() {
            noteEntityA = makeMe.aNote().please();
            noteEntityB = makeMe.aNote().please();
        }

        @Nested
        class Related {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteEntityA).linkTo(noteEntityB, RELATED_TO).please();
            }


            @Test
            void AIsRelatedToB() {
                assertThat(noteEntityA.linkTypes(), contains(RELATED_TO.label));
                assertThat(noteEntityA.linksOfType(RELATED_TO.label), contains(noteEntityB));
            }

            @Test
            void BIsAlsoRelatedToA() {
                assertThat(noteEntityB.linkTypes(), contains(RELATED_TO.label));
                assertThat(noteEntityB.linksOfType(RELATED_TO.label), contains(noteEntityA));
            }

        }

        @Nested
        class BelongsTo {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteEntityA).linkTo(noteEntityB, BELONGS_TO).please();
            }

            @Test
            void ABelongToB() {
                assertThat(noteEntityA.linkTypes(), contains(BELONGS_TO.label));
                assertThat(noteEntityA.linksOfType(BELONGS_TO.label), contains(noteEntityB));
            }

            @Test
            void BHasA() {
                assertThat(noteEntityB.linkTypes(), contains(HAS.label));
                assertThat(noteEntityB.linksOfType(HAS.label), contains(noteEntityA));
            }

        }

        @Nested
        class Has {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteEntityA).linkTo(noteEntityB, HAS).please();
            }

            @Test
            void AHasB() {
                assertThat(noteEntityA.linkTypes(), contains(HAS.label));
                assertThat(noteEntityA.linksOfType(HAS.label), contains(noteEntityB));
            }

            @Test
            void BBelongsToA() {
                assertThat(noteEntityB.linkTypes(), contains(BELONGS_TO.label));
                assertThat(noteEntityB.linksOfType(BELONGS_TO.label), contains(noteEntityA));
            }

        }



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
        NoteEntity noteEntity = makeMe.aNote().title(title).description(description).inMemoryPlease();
        assertThat(noteEntity.getClozeDescription(), equalTo(expectedClozeDescription));
    }
}

package com.odde.doughnut.entities;

import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class NoteEntityTest {

    MakeMe makeMe = new MakeMe();
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
            userEntity = makeMe.aUser().with2Notes().please(modelFactoryService);
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
            note.linkToNote(targetNote);
            List<NoteEntity> targetNotes = note.getTargetNotes();
            assertThat(targetNotes, hasSize(equalTo(1)));
            assertThat(targetNotes, contains(targetNote));
        }
    }
}

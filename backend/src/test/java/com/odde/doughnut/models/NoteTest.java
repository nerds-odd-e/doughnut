package com.odde.doughnut.models;

import com.odde.doughnut.repositories.UserRepository;
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

public class NoteTest {

    MakeMe makeMe = new MakeMe();
    User user;

    @Nested
    class ValidationTest {
        private Validator validator;
        private final Note note = makeMe.aNote().inMemoryPlease();

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

        private Set<ConstraintViolation<Note>> getViolations() {
            return validator.validate(note);
        }

    }

    @Nested
    class NoteWithUser {
        @Autowired private UserRepository userRepository;

        @BeforeEach
        void setup() {
            user = makeMe.aUser().with2Notes().please(userRepository);
        }

        @Test
        void thereShouldBe2NodesForUser() {
            List<Note> notes = user.getNotes();
            assertThat(notes, hasSize(equalTo(2)));
        }

        @Test
        void targetIsEmptyByDefault() {
            Note note = user.getNotes().get(0);
            assertThat(note.getTargetNotes(), is(empty()));
        }

        @Test
        void targetOfLinkedNotes() {
            Note note = user.getNotes().get(0);
            Note targetNote = user.getNotes().get(1);
            note.linkToNote(targetNote);
            List<Note> targetNotes = note.getTargetNotes();
            assertThat(targetNotes, hasSize(equalTo(1)));
            assertThat(targetNotes, contains(targetNote));
        }
    }
}

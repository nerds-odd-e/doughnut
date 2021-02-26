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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class UserNoteTest {
    @Autowired private UserRepository userRepository;

    MakeMe makeMe;
    User user;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
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

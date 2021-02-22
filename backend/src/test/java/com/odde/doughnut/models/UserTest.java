package com.odde.doughnut.models;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class UserTest {
    @Autowired private UserRepository userRepository;
    @Autowired private NoteRepository noteRepository;

    private MakeMe makeMe;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
    }

    @Test
    void shouldReturnEmptyListWhenThhereIsNoNode() {
        User user = makeMe.aUser().inMemoryPlease();
        assertEquals(0, user.getNotesInDescendingOrder().size());
    }

    @Test
    void shouldReturnTheNoteWhenThereIsOne() {
        User user = makeMe.aUser().please(userRepository);
        Note note = makeMe.aNote().forUser(user).please(noteRepository);
        assertThat(user.getNotesInDescendingOrder(), contains(note));
    }

    @Test
    void shouldReturnTheNoteWhenThereIsTwo() {
        User user = makeMe.aUser().please(userRepository);
        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1));
        Note note1 = makeMe.aNote().forUser(user).updatedAt(yesterday).please(noteRepository);
        Note note2 = makeMe.aNote().forUser(user).please(noteRepository);

        assertEquals(note2.getTitle(), user.getNotesInDescendingOrder().get(0).getTitle());
    }
}
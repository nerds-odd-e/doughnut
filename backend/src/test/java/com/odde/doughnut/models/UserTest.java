package com.odde.doughnut.models;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired private SessionFactory sessionFactory;
    Session session;
    MakeMe makeMe;

    @BeforeEach
    void setup() {
        session = sessionFactory.openSession();
        makeMe = new MakeMe(session);
    }

    @Test
    void shouldReturnEmptyListWhenThhereIsNoNode() {
        User user = makeMe.aUser().inMemoryPlease();
        assertEquals(0, user.getNotesInDescendingOrder().size());
    }

    @Test
    void shouldReturnTheNoteWhenThereIsOne() {
        User user = makeMe.aUser().please();
        Note note = makeMe.aNote().forUser(user).please();
        assertThat(user.getNotesInDescendingOrder(), contains(note));
    }

    @Test
    void shouldReturnTheNoteWhenThereIsTwo() {
        User user = makeMe.aUser().please();
        Date yesterday = Date.valueOf(LocalDate.now().minusDays(1));
        Note note1 = makeMe.aNote().forUser(user).updatedAt(yesterday).please();
        Note note2 = makeMe.aNote().forUser(user).please();

        assertEquals(note2.getTitle(), user.getNotesInDescendingOrder().get(0).getTitle());
    }
}
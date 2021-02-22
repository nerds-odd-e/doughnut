package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.RedirectView;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class NoteRestControllerTests {
    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SessionFactory sessionFactory;

    private MakeMe makeMe;
    private User user;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        Session session = sessionFactory.openSession();
        makeMe = new MakeMe(session);
        user = makeMe.aUser().please();
        user = userRepository.findByExternalIdentifier(user.getExternalIdentifier());
        noteController = new NoteRestController(noteRepository, null, new TestCurrentUser(user));
    }

    void shouldBeAbleToSaveNoteWhenThereIsValidUser() {
        Note newNote = makeMe.aNote().inMemoryPlease();
        RedirectView response = noteController.createNote(newNote);
        assertEquals("/review", response.getUrl());
    }

    @Test
    void shouldNotBeAbleToSaveNoteWhenThereIsInvalidUser() {
        Note newNote = new Note();

        try {
            noteController.createNote(newNote);
        } catch (Exception ignored) {
        }

        assertEquals(null, newNote.getId());
    }

    @Test
    void shouldGetListOfNotes() throws Exception {
        Note note = makeMe.aNote().forUser(user).please();
        assertEquals(note.getTitle(), noteController.getNotes(user).get(0).getTitle());
    }
}

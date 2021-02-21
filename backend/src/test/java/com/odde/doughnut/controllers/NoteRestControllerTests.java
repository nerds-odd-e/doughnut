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

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class NoteRestControllerTests {
    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SessionFactory sessionFactory;
    @Mock Model model;

    private Session session;
    private MakeMe makeMe;
    private User currentUser;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe(userRepository);
        currentUser = makeMe.aUser().please();
        noteController = new NoteRestController(noteRepository, userRepository, null );
    }
    @BeforeEach
    void setupSession() {
        session = sessionFactory.openSession();
    }

    @Test
    void shouldBeAbleToSaveNoteWhenThereIsValidUser() {
        Note newNote = makeMe.aNote().inMemoryPlease();
        RedirectView response = noteController.createNote(currentUser, newNote);
        assertEquals("/review", response.getUrl());
    }

    @Test
    void shouldNotBeAbleToSaveNoteWhenThereIsInvalidUser() {
        Note newNote = new Note();

        try {
            noteController.createNote(currentUser, newNote);
        } catch (Exception ignored) {
        }

        assertEquals(null, newNote.getId());
    }

    @Test
    void shouldGetListOfNotes() throws Exception {
        User user = createUser();
        Note note = createNote(user);
        user = userRepository.findByExternalIdentifier(user.getExternalIdentifier());
        assertEquals(note.getTitle(), noteController.getNotes(user).get(0).getTitle());
    }

    @Test
    void shouldGetListOfNotesWithMock() throws Exception {
        UserRepository mockUserRepo = mock(UserRepository.class);
        User user = mock(User.class);
        when(mockUserRepo.findByExternalIdentifier(any())).thenReturn(user);
        when(user.getNotesInDescendingOrder()).thenReturn(Arrays.asList(new Note()));

        List<Note> notes = noteController.getNotes(user);
        verify(user).getNotesInDescendingOrder();
        assertEquals(1, notes.size());
    }

    private Note createNote(User user) {
        Note note = new Note();
        note.setUser(user);
        note.setTitle("Sedition");
        note.setDescription("Incite violence");
        note.setCreatedDatetime(new Date());

        session.save(note);
        return note;
    }

    private User createUser() {
        User user = new User();
        user.setExternalIdentifier("1234567");
        user.setName("my name");
        session.save(user);
        return user;
    }

}

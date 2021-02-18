package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.RedirectView;

import javax.transaction.Transactional;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class NoteControllerTests {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionFactory sessionFactory;

    Session session;

    @Test
    void shouldBeAbleToSaveNoteWhenThereIsValidUser() throws Exception {
        Model model = mock(Model.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        NoteController noteController = new NoteController(noteRepository, createMockUserRepository(new User()));

        RedirectView note = noteController.createNote(createLogin(), new Note(), model);
        assertEquals(note.getUrl(), "/review");
    }

    @Test
    void shouldNotBeAbleToSaveNoteWhenThereIsInvalidUser() {
        Model model = mock(Model.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        NoteController noteController = new NoteController(noteRepository, createMockUserRepository(null));

        Note note = new Note();

        try {
            noteController.createNote(createLogin(), note, model);
        } catch (Exception e) {
            Mockito.verify(noteRepository, times(0)).save(note);
        }
    }

    @BeforeEach
    void setupSession() {
        session = sessionFactory.openSession();
    }

    @Test
    void shouldGetListOfNotes() throws Exception {
        User user = createUser();
        Note note = createNote(user);
        NoteController noteController = new NoteController(noteRepository, userRepository);
        assertEquals(note.getTitle(), noteController.getNotes(createLogin()).get(0).getTitle());
    }

    @Test
    void shouldGetListOfNotesWithMock() throws Exception {
        UserRepository mockUserRepo = mock(UserRepository.class);
        User user = mock(User.class);
        when(mockUserRepo.findByExternalIdentifier(any())).thenReturn(user);
        when(user.getNotesInDescendingOrder()).thenReturn(Arrays.asList(new Note()));

        NoteController noteController = new NoteController(noteRepository, mockUserRepo);
        List<Note> notes = noteController.getNotes(createLogin());
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

    private UserRepository createMockUserRepository(User user) {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByExternalIdentifier("1234567")).thenReturn(user);
        return userRepository;
    }

    private Principal createLogin() {
        return (UserPrincipal) () -> "1234567";
    }
}

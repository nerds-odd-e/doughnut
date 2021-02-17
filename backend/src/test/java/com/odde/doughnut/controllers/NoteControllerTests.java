package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.RedirectView;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)

public class NoteControllerTests {

        @Autowired private NoteRepository noteRepository;
        @Autowired private UserRepository userRepository;

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

        private UserRepository createMockUserRepository(User user) {
            UserRepository userRepository = mock(UserRepository.class);
            when(userRepository.findByExternalIdentifier("1234567")).thenReturn(user);
            return userRepository;
        }

        private Principal createLogin() {
            return (UserPrincipal) () -> "1234567";
        }
}

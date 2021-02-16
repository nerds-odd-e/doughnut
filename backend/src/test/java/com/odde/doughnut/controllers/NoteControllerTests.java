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
        void shouldBeAbleToSaveNoteWhenThereIsValidUser() {
            Model model = mock(Model.class);
            NoteRepository noteRepository = mock(NoteRepository.class);
            NoteController noteController = new NoteController(noteRepository, createMockUserRepository(new User()));

            try {
                assertEquals(noteController.createNote(createUser(), createNote(), model).getUrl(), "/review");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenThereIsInvalidUser() {
            Model model = mock(Model.class);
            NoteRepository noteRepository = mock(NoteRepository.class);
            NoteController noteController = new NoteController(noteRepository, createMockUserRepository(null));

            Note note = createNote();

            try {
                noteController.createNote(createUser(), note, model);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Mockito.verify(noteRepository, times(0)).save(note);

        }

        private UserRepository createMockUserRepository(User user) {
            UserRepository userRepository = mock(UserRepository.class);
            when(userRepository.findByExternalIdentifier("1234567")).thenReturn(user);
            return userRepository;
        }

        private Principal createUser() {
            Principal user = (UserPrincipal) () -> "1234567";
            return user;
        }
        
        private Note createNote() {
            return new Note ()
            {
                public int getId(){
                    return 1;
                }
                public String getTitle(){
                    return "testTitle";
                }
                public String getDescription(){
                    return "testDescription";
                }
            };
        }
}

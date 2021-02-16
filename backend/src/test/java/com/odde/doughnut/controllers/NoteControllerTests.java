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
            UserRepository userRepository = mock(UserRepository.class);
            when(userRepository.findByExternalIdentifier("1234567")).thenReturn(new User());
            NoteController noteController = new NoteController(noteRepository, userRepository);
            Principal user = (UserPrincipal) () -> "1234567";
            Note note = new Note ()
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

            try {
                assertEquals(noteController.createNote(user, note, model).getUrl(), "/review");
            } catch (Exception e) {
                e.printStackTrace();
            }
            verify(noteRepository, times(1)).save(note);

        }

    @Test
    void shouldNotBeAbleToSaveNoteWhenThereIsInvalidUser() {
        Model model = mock(Model.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByExternalIdentifier("1234567")).thenReturn(null);
        NoteController noteController = new NoteController(noteRepository, userRepository);
        Principal user = (UserPrincipal) () -> "1234567";
        Note note = new Note ()
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

        try {
            noteController.createNote(user, note, model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(noteRepository, times(0)).save(note);

    }
}

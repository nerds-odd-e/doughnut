package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.LinkRepository;
import com.odde.doughnut.repositories.NoteRepository;
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
import org.springframework.web.servlet.view.RedirectView;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
public class NoteRestControllerTests {
    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired EntityManager entityManager;
    private MakeMe makeMe;
    private User user;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        user = makeMe.aUser().please(userRepository);
        noteController = new NoteRestController(noteRepository, null, new TestCurrentUser(user));
    }

    @Test
    void shouldGetListOfNotes() throws Exception {
        Note note = makeMe.aNote().forUser(user).please(noteRepository);
        makeMe.refresh(entityManager, user);
        assertEquals(note.getTitle(), noteController.getNotes().get(0).getTitle());
    }

    @Nested
    class createNoteTest {

        @Test
        void shouldBeAbleToSaveNoteWhenThereIsValidUser() {
            Note newNote = makeMe.aNote().inMemoryPlease();
            RedirectView response = noteController.createNote(newNote);
            assertEquals("/notes/" + newNote.getId(), response.getUrl());
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
    }

    @Nested
    class DeleteNoteTest {
        @Autowired private LinkRepository linkRepository;

        @Test
        void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please(userRepository);
            Note note = makeMe.aNote().forUser(anotherUser).please(noteRepository);
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, ()->
                    noteController.deleteNote(noteId)
                    );
            assertTrue(noteRepository.findById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            Note note = makeMe.aNote().forUser(user).please(noteRepository);
            Integer noteId = note.getId();
            RedirectView response = noteController.deleteNote(noteId);
            assertEquals("/notes", response.getUrl());
            assertFalse(noteRepository.findById(noteId).isPresent());
            assertTrue(userRepository.findById(user.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            Note parent = makeMe.aNote().forUser(user).please(noteRepository);
            Note subject = makeMe.aNote().under(parent).forUser(user).please(noteRepository);
            Note sibling = makeMe.aNote().under(parent).forUser(user).please(noteRepository);
            Note child = makeMe.aNote().under(subject).forUser(user).please(noteRepository);

            noteController.deleteNote(subject.getId());

            assertTrue(noteRepository.findById(sibling.getId()).isPresent());
            assertFalse(noteRepository.findById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheLinkToAndFromThisNote() throws NoAccessRightException {
            Note referTo = makeMe.aNote().forUser(user).please(noteRepository);
            Note subject = makeMe.aNote().forUser(user).linkTo(referTo).please(noteRepository);
            Note referFrom = makeMe.aNote().forUser(user).linkTo(subject).linkTo(referTo).please(noteRepository);
            long oldCount = linkRepository.count();

            noteController.deleteNote(subject.getId());

            assertThat(makeMe.refresh(entityManager, referFrom).getId(), is(not(nullValue())));
            assertThat(makeMe.refresh(entityManager, referTo).getId(), is(not(nullValue())));
            assertThat(linkRepository.count(), equalTo(oldCount - 2));
        }
    }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.LinkRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;
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
    @Autowired EntityManager entityManager;
    @Autowired ModelFactoryService modelFactoryService;
    private MakeMe makeMe;
    private User user;
    private NoteRestController noteController;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        user = makeMe.aUser().please(modelFactoryService);
        noteController = new NoteRestController(new TestCurrentUser(user), modelFactoryService);
    }

    @Test
    void shouldGetListOfNotes() {
        Note note = makeMe.aNote().forUser(user).please(noteRepository);
        makeMe.refresh(entityManager, user);
        assertEquals(note.getTitle(), noteController.getNotes().get(0).getTitle());
    }


    @Nested
    class DeleteNoteTest {
        @Autowired private LinkRepository linkRepository;

        @Test
        void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please(modelFactoryService);
            Note note = makeMe.aNote().forUser(anotherUser).please(noteRepository);
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, ()->
                    noteController.deleteNote(note)
                    );
            assertTrue(noteRepository.findById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            Note note = makeMe.aNote().forUser(user).please(noteRepository);
            Integer noteId = note.getId();
            RedirectView response = noteController.deleteNote(note);
            assertEquals("/notes", response.getUrl());
            assertFalse(noteRepository.findById(noteId).isPresent());
            assert(modelFactoryService.findUserById(note.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            Note parent = makeMe.aNote().forUser(user).please(noteRepository);
            Note subject = makeMe.aNote().under(parent).forUser(user).please(noteRepository);
            Note sibling = makeMe.aNote().under(parent).forUser(user).please(noteRepository);
            Note child = makeMe.aNote().under(subject).forUser(user).please(noteRepository);

            noteController.deleteNote(subject);

            assertTrue(noteRepository.findById(sibling.getId()).isPresent());
            assertFalse(noteRepository.findById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheLinkToAndFromThisNote() throws NoAccessRightException {
            Note referTo = makeMe.aNote().forUser(user).please(noteRepository);
            Note subject = makeMe.aNote().forUser(user).linkTo(referTo).please(noteRepository);
            Note referFrom = makeMe.aNote().forUser(user).linkTo(subject).linkTo(referTo).please(noteRepository);
            long oldCount = linkRepository.count();

            noteController.deleteNote(subject);

            assertThat(makeMe.refresh(entityManager, referFrom).getId(), is(not(nullValue())));
            assertThat(makeMe.refresh(entityManager, referTo).getId(), is(not(nullValue())));
            assertThat(linkRepository.count(), equalTo(oldCount - 2));
        }
    }
}

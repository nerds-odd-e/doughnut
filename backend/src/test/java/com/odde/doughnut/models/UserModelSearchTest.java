package com.odde.doughnut.models;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SearchTerm;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelSearchTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;
    Note note;
    final SearchTerm searchTerm = new SearchTerm();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
        note = makeMe.aNote().byUser(userModel).please();
    }

    private List<Note> search() {
        makeMe.refresh(note.getNotebook());
        return userModel.filterLinkableNotes(note, searchTerm);
    }

    @Test
    void returnNullWhenNoteKeyIsGiven() {
        final List<Note> notes = search();
        assertThat(notes, is(nullValue()));
    }

    @Test
    void theNoteItselfIsNotIncludedInTheResult() {
        searchTerm.setSearchKey(note.getTitle());
        final List<Note> notes = search();
        assertTrue(notes.isEmpty());
    }

    @Test
    void theSearchIsCaseInsensitive() {
        Note anotherNote = makeMe.aNote("Some Note").under(note).please();
        searchTerm.setSearchKey("not");
        final List<Note> notes = search();
        assertThat(notes, contains(anotherNote));
    }

    @Test
    void theSearchShouldNotIncludeNoteFromOtherNotebook() {
        Note anotherNote = makeMe.aNote("Some Note").byUser(userModel).please();
        searchTerm.setSearchKey(anotherNote.getTitle());
        final List<Note> notes = search();
        assertTrue(notes.isEmpty());
    }

    @Test
    void theSearchShouldIncludeNoteFromOtherNotebookIfGlobally() {
        Note anotherNote = makeMe.aNote("Some Note").byUser(userModel).please();
        searchTerm.setSearchKey(anotherNote.getTitle());
        searchTerm.setSearchGlobally(true);
        final List<Note> notes = search();
        assertThat(notes, contains(anotherNote));
    }

    @Nested
    class ThereIsANoteBookInBazaar {
        Note bazaarNote;

        @BeforeEach
        void setup() {
            bazaarNote = makeMe.aNote().byUser(anotherUser).please();
            makeMe.aBazaarNodebook(bazaarNote.getNotebook()).please();
        }

        @Test
        void theSearchShouldNotIncludeNoteInBazaar() {
            searchTerm.setSearchKey(bazaarNote.getTitle());
            searchTerm.setSearchGlobally(true);
            final List<Note> notes = search();
            assertTrue(notes.isEmpty());
        }

        @Test
        void theSearchShouldIncludeNoteISubscribed() {
            makeMe.aSubscription().forNotebook(bazaarNote.getNotebook()).forUser(userModel.getEntity()).please();
            searchTerm.setSearchKey(bazaarNote.getTitle());
            searchTerm.setSearchGlobally(true);
            final List<Note> notes = search();
            assertThat(notes, contains(bazaarNote));
        }

    }

    @Nested
    class ThereIsANoteBookInMyCircle {
        Note circleNote;

        @BeforeEach
        void setup() {
            Circle circle = makeMe.aCircle().hasMember(userModel).hasMember(anotherUser).please();
            circleNote = makeMe.aNote().byUser(anotherUser).inCircle(circle).please();
        }

        @Test
        void theSearchShouldIncludeNote() {
            searchTerm.setSearchKey(circleNote.getTitle());
            searchTerm.setSearchGlobally(true);
            final List<Note> notes = search();
            assertThat(notes, contains(circleNote));
        }

    }
}


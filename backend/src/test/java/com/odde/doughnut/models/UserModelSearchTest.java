package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.SearchTerm;
import com.odde.doughnut.testability.MakeMe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelSearchTest {
    @Autowired
    MakeMe makeMe;
    SearchTermModel searchTermModel;
    User user;
    UserModel anotherUser;
    Note note;
    final SearchTerm searchTerm = new SearchTerm();

    @BeforeEach
    void setup() {
        user  = makeMe.aUser().please();
        note = makeMe.aNote().byUser(user).please();
        searchTerm.note = Optional.of(note);
        searchTermModel = new SearchTermModel(user, makeMe.modelFactoryService, searchTerm);
        anotherUser = makeMe.aUser().toModelPlease();
    }

    private List<Note> search() {
        return searchTermModel.searchForNotes();
    }

    @Test
    void returnNullWhenNoteKeyIsGiven() {
        assertThat(search(), is(nullValue()));
    }

    @Test
    void theNoteItselfIsNotIncludedInTheResult() {
        searchTerm.setSearchKey(note.getTitle());
        assertTrue(search().isEmpty());
    }

    @Test
    void theSearchIsCaseInsensitive() {
        Note anotherNote = makeMe.aNote("Some Note").under(note).please();
        searchTerm.setSearchKey("not");
        assertThat(search(), contains(anotherNote));
    }

    @Test
    void theSearchShouldNotIncludeNoteFromOtherNotebook() {
        Note anotherNote = makeMe.aNote("Some Note").byUser(user).please();
        searchTerm.setSearchKey(anotherNote.getTitle());
        assertTrue(search().isEmpty());
    }

    @Test
    void theSearchShouldIncludeNoteFromOtherNotebookIfGlobally() {
        Note anotherNote = makeMe.aNote("Some Note").byUser(user).please();
        searchTerm.setSearchKey(anotherNote.getTitle());
        searchTerm.setAllMyNotebooksAndSubscriptions(true);
        assertThat(search(), contains(anotherNote));
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
            searchTerm.setAllMyNotebooksAndSubscriptions(true);
            assertTrue(search().isEmpty());
        }

        @Test
        void theSearchShouldIncludeNoteISubscribed() {
            makeMe.aSubscription().forNotebook(bazaarNote.getNotebook()).forUser(user).please();
            searchTerm.setSearchKey(bazaarNote.getTitle());
            searchTerm.setAllMyNotebooksAndSubscriptions(true);
            assertThat(search(), contains(bazaarNote));
        }

    }

    @Nested
    class ThereIsANoteBookInMyCircle {
        Note circleNote;

        @BeforeEach
        void setup() {
            Circle circle = makeMe.aCircle().hasMember(user).hasMember(anotherUser).please();
            circleNote = makeMe.aNote().byUser(anotherUser).inCircle(circle).please();
        }

        @Test
        void theSearchShouldNotIncludeNoteIfNotSearchingInCircle() {
            searchTerm.setSearchKey(circleNote.getTitle());
            searchTerm.setAllMyNotebooksAndSubscriptions(true);
            assertThat(search(), not(contains(circleNote)));
        }

        @Test
        void theSearchShouldIncludeNote() {
            searchTerm.setAllMyCircles(true);
            searchTerm.setSearchKey(circleNote.getTitle());
            searchTerm.setAllMyNotebooksAndSubscriptions(true);
            assertThat(search(), contains(circleNote));
        }

    }
}


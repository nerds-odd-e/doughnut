package com.odde.doughnut.services;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class NoteServiceTest {
    @Autowired private NoteRepository noteRepository;
    @Autowired EntityManager entityManager;


    MakeMe makeMe;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
    }

    @Nested
    class GetAncestors {
        Note topLevel;

        @BeforeEach
        void setup() {
            topLevel = makeMe.aNote().please(noteRepository);
        }

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            NoteService noteService = new NoteService(noteRepository, topLevel);
            List<Note> ancestors = noteService.getAncestors();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            Note subject = makeMe.aNote().under(topLevel).please(noteRepository);
            Note sibling = makeMe.aNote().under(topLevel).please(noteRepository);

            NoteService noteService = new NoteService(noteRepository, subject);
            List<Note> ancestry = noteService.getAncestors();
            assertThat(ancestry, contains(topLevel, subject));
            assertThat(ancestry, not(contains(sibling)));
        }

    }

}

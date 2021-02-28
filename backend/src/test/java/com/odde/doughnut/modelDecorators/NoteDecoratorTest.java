package com.odde.doughnut.modelDecorators;

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

public class NoteDecoratorTest {
    Note topLevel;
    @Autowired private NoteRepository noteRepository;
    @Autowired EntityManager entityManager;


    MakeMe makeMe;

    NoteDecorator decorate(Note subjectNote) {
        return new NoteDecorator(noteRepository, subjectNote);
    }

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        topLevel = makeMe.aNote().please(noteRepository);
    }

    @Nested
    class GetAncestors {

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            NoteDecorator decoratedNote = decorate(topLevel);
            List<Note> ancestors = decoratedNote.getAncestors();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            Note subject = makeMe.aNote().under(topLevel).please(noteRepository);
            Note sibling = makeMe.aNote().under(topLevel).please(noteRepository);

            NoteDecorator decoratedNote = decorate(subject);
            List<Note> ancestry = decoratedNote.getAncestors();
            assertThat(ancestry, contains(topLevel, subject));
            assertThat(ancestry, not(contains(sibling)));
        }

    }

    @Nested
    class Navigations {

        @Test
        void topNodeHasNoSiblings() {
            Note subjectNote = makeMe.aNote().please(noteRepository);
            Note nextTopLevel = makeMe.aNote().please(noteRepository);
            NoteDecorator subject = decorate(subjectNote);

            assertNavigation(subject, null, null, null, null);
        }

        private void assertNavigation(NoteDecorator subject, Note previousSibling, Note previous, Note next, Note nextSibling) {
            assertThat(subject.getPreviousSiblingNote(), equalTo(previousSibling));
            assertThat(subject.getPreviousNote(), equalTo(previous));
            assertThat(subject.getNextNote(), equalTo(next));
            assertThat(subject.getNextSiblingNote(), equalTo(nextSibling));
        }

    }
}

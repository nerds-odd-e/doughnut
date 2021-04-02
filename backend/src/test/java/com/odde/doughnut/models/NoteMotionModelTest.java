package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteMotionModelTest {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired MakeMe makeMe;
    NoteEntity topNote;
    NoteEntity firstChild;
    NoteEntity secondChild;

    @BeforeEach
    void setup() {
        topNote = makeMe.aNote("topNote").please();
        firstChild = makeMe.aNote("firstChild").under(topNote).please();
        secondChild = makeMe.aNote("secondChild").under(topNote).please();
        makeMe.refresh(topNote);
        makeMe.refresh(firstChild);
        makeMe.refresh(secondChild);
    }

    void move(NoteEntity subject, NoteEntity relativeNote, boolean asFirstChildOfNote) throws CyclicLinkDetectedException {
        NoteMotionEntity motion = new NoteMotionEntity(relativeNote, asFirstChildOfNote);
        NoteMotionModel noteMotionModel = modelFactoryService.toNoteMotionModel(motion, subject);
        noteMotionModel.execute();
    }

    @Test
    void moveBehind() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, false);
        assertOrder(secondChild, firstChild);
    }

    private void assertOrder(NoteEntity note1, NoteEntity note2) {
        NoteEntity parentNote = note1.getParentNote();
        makeMe.refresh(parentNote);
        assertThat(parentNote.getChildren(), containsInRelativeOrder(note1, note2));
    }

    @Test
    void moveSecondBehindFirst() throws CyclicLinkDetectedException {
        move(secondChild, firstChild, false);
        assertOrder(firstChild, secondChild);
    }

    @Test
    void moveSecondToBeTheFirstSibling() throws CyclicLinkDetectedException {
        move(secondChild, topNote, true);
        assertOrder(secondChild, firstChild);
    }

    @Test
    void moveUnder() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, true);
        assertThat(firstChild.getParentNote(), equalTo(secondChild));
    }

    @Test
    void moveBothToTheEndInSequence() throws CyclicLinkDetectedException {
        move(firstChild, secondChild, false);
        move(secondChild, firstChild, false);
        assertOrder(firstChild, secondChild);
    }

    @Nested
    class WhenThereIsAThirdLevel {
        NoteEntity thirdLevel;
        NoteEntity forthLevel;

        @BeforeEach
        void setup() {
            thirdLevel = makeMe.aNote("thirdLevel").under(firstChild).please();
            forthLevel = makeMe.aNote("forthLevel").under(thirdLevel).please();
        }

        @Test
        void moveAfterNoteOfDifferentLevel() throws CyclicLinkDetectedException {
            move(secondChild, thirdLevel, false);
            assertThat(secondChild.getParentNote(), equalTo(firstChild));
        }

        @Test
        void moveToOwnDescendentIsNotAllowed() {
            assertThrows(CyclicLinkDetectedException.class, ()->
                move(topNote, thirdLevel, false)
            );
        }

        @Test
        void moveWithOwnChild() throws CyclicLinkDetectedException {
            move(firstChild, secondChild, true);
            assertThat(firstChild.getAncestorsIncludingMe(), contains(topNote, secondChild, firstChild));
            assertThat(thirdLevel.getAncestorsIncludingMe(), contains(topNote, secondChild, firstChild, thirdLevel));
            assertThat(forthLevel.getAncestorsIncludingMe(), contains(topNote, secondChild, firstChild, thirdLevel, forthLevel));
        }

    }

    @Nested
    class WhenThereIsAThirdChild {
        NoteEntity thirdChild;

        @BeforeEach
        void setup() {
            thirdChild = makeMe.aNote().under(topNote).please();
        }

        @Test
        void moveBetweenSecondAndThird() throws CyclicLinkDetectedException {
            move(firstChild, secondChild, false);
            assertOrder(secondChild, firstChild);
            assertOrder(firstChild, thirdChild);
        }
    }

}


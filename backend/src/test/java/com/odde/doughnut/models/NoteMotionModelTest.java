package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteMotionModelTest {
    NoteEntity topLevel;
    @Autowired
    ModelFactoryService modelFactoryService;

    MakeMe makeMe = new MakeMe();
    NoteEntity topNote;
    NoteEntity firstChild;
    NoteEntity secondChild;

    @BeforeEach
    void setup() {
        topNote = makeMe.aNote().please(modelFactoryService);
        firstChild = makeMe.aNote().under(topNote).please(modelFactoryService);
        secondChild = makeMe.aNote().under(topNote).please(modelFactoryService);
    }

    void move(NoteEntity subject, NoteEntity relativeNote, boolean asFirstChildOfNote) {
        NoteMotionEntity motion = new NoteMotionEntity(relativeNote, asFirstChildOfNote);
        NoteMotionModel noteMotionModel = modelFactoryService.toNoteMotionModel(motion, subject);
        noteMotionModel.execute();
    }

    @Test
    void moveBehind() {
        move(firstChild, secondChild, false);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveSecondBehindFirst() {
        move(secondChild, firstChild, false);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Test
    void moveSecondBehindItself() {
        move(secondChild, secondChild, false);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Test
    void moveSecondToBeTheFirstSibling() {
        move(secondChild, topNote, true);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveUnder() {
        move(firstChild, secondChild, true);
        assertThat(firstChild.getParentNote(), equalTo(secondChild));
    }

    @Test
    void moveAfterNoteOfDifferentLevel() {
        NoteEntity thirdLevel = makeMe.aNote().under(firstChild).please(modelFactoryService);
        move(secondChild, thirdLevel, false);
        assertThat(secondChild.getParentNote(), equalTo(firstChild));
    }

    @Test
    void moveBothToTheEndInSequence() {
        move(firstChild, secondChild, false);
        move(secondChild, firstChild, false);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Nested
    class WhenThereIsAThirdChild {
        NoteEntity thirdChild;

        @BeforeEach
        void setup() {
            thirdChild = makeMe.aNote().under(topNote).please(modelFactoryService);
        }

        @Test
        void moveBetweenSecondAndThird() {
            move(firstChild, secondChild, false);
            assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
            assertThat(firstChild.getSiblingOrder(), is(lessThan(thirdChild.getSiblingOrder())));
        }
    }

}


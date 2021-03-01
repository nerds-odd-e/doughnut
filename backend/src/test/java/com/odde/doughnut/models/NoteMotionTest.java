package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
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
public class NoteMotionTest {
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

    @Test
    void moveBehind() {
        NoteMotion motion = new NoteMotion(secondChild, false);
        motion.execute(firstChild, modelFactoryService);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveSecondBehindFirst() {
        NoteMotion motion = new NoteMotion(firstChild, false);
        motion.execute(secondChild, modelFactoryService);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Test
    void moveSecondBehindItself() {
        NoteMotion motion = new NoteMotion(secondChild, false);
        motion.execute(secondChild, modelFactoryService);
        assertThat(firstChild.getSiblingOrder(), is(lessThan(secondChild.getSiblingOrder())));
    }

    @Test
    void moveSecondToBeTheFirstSibling() {
        NoteMotion motion = new NoteMotion(topLevel, true);
        motion.execute(secondChild, modelFactoryService);
        assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
    }

    @Test
    void moveUnder() {
        NoteMotion motion = new NoteMotion(secondChild, true);
        motion.execute(firstChild, modelFactoryService);
        assertThat(firstChild.getParentNote(), equalTo(secondChild));
    }

    @Test
    void moveAfterNoteOfDifferentLevel() {
        NoteEntity thirdLevel = makeMe.aNote().under(firstChild).please(modelFactoryService);
        NoteMotion motion = new NoteMotion(thirdLevel, false);
        motion.execute(secondChild, modelFactoryService);
        assertThat(secondChild.getParentNote(), equalTo(firstChild));
    }

    @Test
    void moveBothToTheEndInSequence() {
        NoteMotion motion = new NoteMotion(secondChild, false);
        motion.execute(firstChild, modelFactoryService);

        NoteMotion motion2 = new NoteMotion(firstChild, false);
        motion2.execute(secondChild, modelFactoryService);

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
            NoteMotion motion = new NoteMotion(secondChild, false);
            motion.execute(firstChild, modelFactoryService);
            assertThat(secondChild.getSiblingOrder(), is(lessThan(firstChild.getSiblingOrder())));
            assertThat(firstChild.getSiblingOrder(), is(lessThan(thirdChild.getSiblingOrder())));
        }
    }

}


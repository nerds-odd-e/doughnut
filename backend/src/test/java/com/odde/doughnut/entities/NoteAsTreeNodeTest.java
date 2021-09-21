package com.odde.doughnut.entities;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteAsTreeNodeTest {
    Note topLevel;
    @Autowired MakeMe makeMe;

    @BeforeEach
    void setup() {
        topLevel = makeMe.aNote("topLevel").please();
    }

    @Nested
    class GetAncestors {

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            List<Note> ancestors = topLevel.getAncestorsIncludingMe();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            Note subject = makeMe.aNote("subject").under(topLevel).please();
            Note sibling = makeMe.aNote("sibling").under(topLevel).please();

            List<Note> ancestry = subject.getAncestorsIncludingMe();
            assertThat(ancestry, contains(topLevel, subject));
            assertThat(ancestry, not(contains(sibling)));
        }
    }
}

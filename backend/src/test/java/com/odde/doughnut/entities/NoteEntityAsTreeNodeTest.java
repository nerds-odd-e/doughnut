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
public class NoteEntityAsTreeNodeTest {
    NoteEntity topLevel;
    @Autowired MakeMe makeMe;

    @BeforeEach
    void setup() {
        topLevel = makeMe.aNote("topLevel").please();
    }

    @Nested
    class GetAncestors {

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            List<NoteEntity> ancestors = topLevel.getAncestorsIncludingMe();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            NoteEntity subject = makeMe.aNote("subject").under(topLevel).please();
            NoteEntity sibling = makeMe.aNote("sibling").under(topLevel).please();

            List<NoteEntity> ancestry = subject.getAncestorsIncludingMe();
            assertThat(ancestry, contains(topLevel, subject));
            assertThat(ancestry, not(contains(sibling)));
        }
    }

    @Nested
    class Navigations {

        @Test
        void topNoteHasNoSiblings() {
            NoteEntity subjectNote = makeMe.aNote().please();
            NoteEntity nextTopLevel = makeMe.aNote().please();

            assertNavigation(subjectNote, null, null, null, null);
        }

        @Nested
        class TopLevelNoteHasAChild {
            NoteEntity child;
            NoteEntity nephew;

            @BeforeEach
            void setup() {
                child = makeMe.aNote().under(topLevel).please();
                nephew = makeMe.aNote().under(topLevel).please();
                makeMe.refresh(nephew);
                makeMe.refresh(topLevel);
                makeMe.refresh(child);
            }

            @Test
            void topLevelHasChildAsNext() {
                assertNavigation(topLevel, null, null, child, null);
            }

            @Test
            void firstChildNote() {
                assertNavigation(child, null, topLevel, nephew, nephew);
            }

            @Test
            void secondChild() {
                assertNavigation(nephew, child, child, null, null);
            }

            @Nested
            class ChildHasGrandchild {
                NoteEntity grandchild;

                @BeforeEach
                void setup() {
                    grandchild = makeMe.aNote().under(child).please();
                    makeMe.refresh(nephew);
                    makeMe.refresh(topLevel);
                    makeMe.refresh(child);
                    makeMe.refresh(grandchild);
                }

                @Test
                void firstChildNote() {
                    assertNavigation(child, null, topLevel, grandchild, nephew);
                }

                @Test
                void secondChild() {
                    assertNavigation(nephew, child, grandchild, null, null);
                }

                @Test
                void grandchildView() {
                    assertNavigation(grandchild, null, child, nephew, null);
                }

            }
        }

        private void assertNavigation(NoteEntity entity, NoteEntity previousSibling, NoteEntity previous, NoteEntity next, NoteEntity nextSibling) {
            assertThat(entity.getPreviousSibling(), equalTo(previousSibling));
            assertThat(entity.getPrevious(), equalTo(previous));
            assertThat(entity.getNext(), equalTo(next));
            assertThat(entity.getNextSibling(), equalTo(nextSibling));
        }

    }
}

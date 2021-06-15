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

    @Nested
    class Navigations {

        @Test
        void topNoteHasNoSiblings() {
            Note subjectNote = makeMe.aNote().please();
            Note nextTopLevel = makeMe.aNote().please();

            assertNavigation(subjectNote, null, null, null, null);
        }

        @Nested
        class TopLevelNoteHasAChild {
            Note child;
            Note nephew;

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
                Note grandchild;

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

        private void assertNavigation(Note entity, Note previousSibling, Note previous, Note next, Note nextSibling) {
            assertThat(entity.getPreviousSibling().orElse(null), equalTo(previousSibling));
            assertThat(entity.getPrevious().orElse(null), equalTo(previous));
            assertThat(entity.getNext().orElse(null), equalTo(next));
            assertThat(entity.getNextSibling().orElse(null), equalTo(nextSibling));
        }

    }
}

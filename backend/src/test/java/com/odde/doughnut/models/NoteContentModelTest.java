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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteContentModelTest {
    NoteEntity topLevel;
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired MakeMe makeMe;

    TreeNodeModel toModel(NoteEntity subjectNote) {
        return modelFactoryService.toTreeNodeModel(subjectNote);
    }

    @BeforeEach
    void setup() {
        topLevel = makeMe.aNote("topLevel").please();
    }

    @Test
    void emptyTopLevelNote() {
        assertThat(toModel(topLevel).getFirstChild(), is(nullValue()));
    }

    @Test
    void topLevelNoteWithOneChild() {
        NoteEntity child = makeMe.aNote().under(topLevel).please();
        makeMe.refresh(topLevel);
        makeMe.refresh(child);
        assertThat(toModel(topLevel).getFirstChild(), equalTo(child));
    }

    @Nested
    class GetAncestors {

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            TreeNodeModel decoratedNote = toModel(topLevel);
            List<NoteEntity> ancestors = decoratedNote.getAncestorsIncludingMe();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            NoteEntity subject = makeMe.aNote("subject").under(topLevel).please();
            NoteEntity sibling = makeMe.aNote("sibling").under(topLevel).please();

            TreeNodeModel decoratedNote = toModel(subject);
            List<NoteEntity> ancestry = decoratedNote.getAncestorsIncludingMe();
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
            TreeNodeModel subject = toModel(subjectNote);

            assertNavigation(subject, null, null, null, null);
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
                TreeNodeModel subject = toModel(topLevel);
                assertNavigation(subject, null, null, child, null);
            }

            @Test
            void firstChildNote() {
                TreeNodeModel subject = toModel(child);
                assertNavigation(subject, null, topLevel, nephew, nephew);
            }

            @Test
            void secondChild() {
                TreeNodeModel subject = toModel(nephew);
                assertNavigation(subject, child, child, null, null);
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
                    TreeNodeModel subject = toModel(child);
                    assertNavigation(subject, null, topLevel, grandchild, nephew);
                }

                @Test
                void secondChild() {
                    TreeNodeModel subject = toModel(nephew);
                    assertNavigation(subject, child, grandchild, null, null);
                }

                @Test
                void grandchildView() {
                    TreeNodeModel subject = toModel(grandchild);
                    assertNavigation(subject, null, child, nephew, null);
                }

            }
        }

        private void assertNavigation(TreeNodeModel subject, NoteEntity previousSibling, NoteEntity previous, NoteEntity next, NoteEntity nextSibling) {
            assertThat(subject.getPreviousSiblingNote(), equalTo(previousSibling));
            assertThat(subject.getPreviousNote(), equalTo(previous));
            assertThat(subject.getNextNote(), equalTo(next));
            assertThat(subject.getNextSiblingNote(), equalTo(nextSibling));
        }

    }
}

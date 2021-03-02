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
public class NoteModelTest {
    NoteEntity topLevel;
    @Autowired ModelFactoryService modelFactoryService;

    MakeMe makeMe;

    NoteModel toModel(NoteEntity subjectNote) {
        return modelFactoryService.toNoteModel(subjectNote);
    }

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        topLevel = makeMe.aNote().please(modelFactoryService);
    }

    @Test
    void emptyTopLevelNote() {
        assertThat(toModel(topLevel).getFirstChild(), is(nullValue()));
    }

    @Test
    void topLevelNoteWithOneChild() {
        NoteEntity child = makeMe.aNote().under(topLevel).please(modelFactoryService);
        assertThat(toModel(topLevel).getFirstChild(), equalTo(child));
    }

    @Nested
    class GetAncestors {

        @Test
        void topLevelNoteHaveEmptyAncestors() {
            NoteModel decoratedNote = toModel(topLevel);
            List<NoteEntity> ancestors = decoratedNote.getAncestors();
            assertThat(ancestors, contains(topLevel));
        }

        @Test
        void childHasParentInAncestors() {
            NoteEntity subject = makeMe.aNote().under(topLevel).please(modelFactoryService);
            NoteEntity sibling = makeMe.aNote().under(topLevel).please(modelFactoryService);

            NoteModel decoratedNote = toModel(subject);
            List<NoteEntity> ancestry = decoratedNote.getAncestors();
            assertThat(ancestry, contains(topLevel, subject));
            assertThat(ancestry, not(contains(sibling)));
        }
    }

    @Nested
    class Navigations {

        @Test
        void topNoteHasNoSiblings() {
            NoteEntity subjectNote = makeMe.aNote().please(modelFactoryService);
            NoteEntity nextTopLevel = makeMe.aNote().please(modelFactoryService);
            NoteModel subject = toModel(subjectNote);

            assertNavigation(subject, null, null, null, null);
        }

        @Nested
        class TopLevelNoteHasAChild {
            NoteEntity child;
            NoteEntity nephew;

            @BeforeEach
            void setup() {
                child = makeMe.aNote().under(topLevel).please(modelFactoryService);
                nephew = makeMe.aNote().under(topLevel).please(modelFactoryService);
            }

            @Test
            void topLevelHasChildAsNext() {
                NoteModel subject = toModel(topLevel);
                assertNavigation(subject, null, null, child, null);
            }

            @Test
            void firstChildNote() {
                NoteModel subject = toModel(child);
                assertNavigation(subject, null, topLevel, nephew, nephew);
            }

            @Test
            void secondChild() {
                NoteModel subject = toModel(nephew);
                assertNavigation(subject, child, child, null, null);
            }

            @Nested
            class ChildHasGrandchild {
                NoteEntity grandchild;

                @BeforeEach
                void setup() {
                    grandchild = makeMe.aNote().under(child).please(modelFactoryService);
                }

                @Test
                void firstChildNote() {
                    NoteModel subject = toModel(child);
                    assertNavigation(subject, null, topLevel, grandchild, nephew);
                }

                @Test
                void secondChild() {
                    NoteModel subject = toModel(nephew);
                    assertNavigation(subject, child, grandchild, null, null);
                }

                @Test
                void grandchildView() {
                    NoteModel subject = toModel(grandchild);
                    assertNavigation(subject, null, child, nephew, null);
                }

            }
        }

        private void assertNavigation(NoteModel subject, NoteEntity previousSibling, NoteEntity previous, NoteEntity next, NoteEntity nextSibling) {
            assertThat(subject.getPreviousSiblingNote(), equalTo(previousSibling));
            assertThat(subject.getPreviousNote(), equalTo(previous));
            assertThat(subject.getNextNote(), equalTo(next));
            assertThat(subject.getNextSiblingNote(), equalTo(nextSibling));
        }

    }
}

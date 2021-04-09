package com.odde.doughnut.entities;

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

import javax.validation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.Link.LinkType.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteTest {

    @Autowired MakeMe makeMe;
    UserEntity userEntity;

    @Test
    void timeOrder() {
        Note parent = makeMe.aNote().please();
        Note note1 = makeMe.aNote().under(parent).please();
        Note note2 = makeMe.aNote().under(parent).please();
        makeMe.refresh(parent);
        assertThat(parent.getChildren(), containsInRelativeOrder(note1, note2));
    }

    @Nested
    class Picture {

        @Test
        void useParentPicture() {
            Note parent = makeMe.aNote().pictureUrl("https://img.com/xxx.jpg").inMemoryPlease();
            Note child = makeMe.aNote().under(parent).useParentPicture().inMemoryPlease();
            assertThat(child.getNotePicture(), equalTo(parent.getNoteContent().getPictureUrl()));
        }
    }

    @Nested
    class ValidationTest {
        private Validator validator;
        private final Note note = makeMe.aNote().inMemoryPlease();

        @BeforeEach
        public void setUp() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        public void defaultNoteFromMakeMeIsValidate() {
            assertThat(getViolations(), is(empty()));
        }

        @Test
        public void titleIsNotOptional() {
            makeMe.theNote(note).title("");
            assertThat(getViolations(), is(not(empty())));
        }

        @Test
        public void titleCannotBeTooLong() {
            makeMe.theNote(note).title(makeMe.aStringOfLength(101));
            assertThat(getViolations(), is(not(empty())));
        }

        @Test
        public void goodMask() {
            note.getNoteContent().setPictureMask("1 -2.3 3 -4");
            assertThat(getViolations(), is(empty()));
        }

        @Test
        public void goodMaskWith2Rect() {
            note.getNoteContent().setPictureMask("-1 2 3 4 11 22 33 44");
            assertThat(getViolations(), is(empty()));
        }

        @Test
        public void masksNeedToBeFourNumbers() {
            note.getNoteContent().setPictureMask("1 2 3 4 5 6 7");
            assertThat(getViolations(), is(not(empty())));
            Path propertyPath = getViolations().stream().findFirst().get().getPropertyPath();
            assertThat(propertyPath.toString(), equalTo("noteContent.pictureMask"));
        }

        @Test
        public void withBothUploadPictureProxyAndPicture() {
            note.getNoteContent().setUploadPictureProxy(makeMe.anUploadedPicture().toMultiplePartFilePlease());
            note.getNoteContent().setPictureUrl("http://url/img");
            assertThat(getViolations(), is(not(empty())));
            List<String> errorFields = getViolations().stream().map(v->v.getPropertyPath().toString()).collect(toList());
            assertThat(errorFields, containsInAnyOrder("noteContent.uploadPicture", "noteContent.pictureUrl"));
        }

        private Set<ConstraintViolation<Note>> getViolations() {
            return validator.validate(note);
        }

    }

    @Nested
    class NoteWithUser {
        @Autowired private ModelFactoryService modelFactoryService;

        @BeforeEach
        void setup() {
            userEntity = makeMe.aUser().with2Notes().please();
        }

        @Test
        void thereShouldBe2NodesForUser() {
            List<Note> notes = userEntity.getNotes();
            assertThat(notes, hasSize(equalTo(2)));
        }

        @Test
        void targetIsEmptyByDefault() {
            Note note = userEntity.getNotes().get(0);
            assertThat(note.getTargetNotes(), is(empty()));
        }

        @Test
        void targetOfLinkedNotes() {
            Note note = userEntity.getNotes().get(0);
            Note targetNote = userEntity.getNotes().get(1);
            makeMe.theNote(note).linkTo(targetNote).please();
            List<Note> targetNotes = note.getTargetNotes();
            assertThat(targetNotes, hasSize(equalTo(1)));
            assertThat(targetNotes, contains(targetNote));
        }
    }

    @Nested
    class LinkTypes {
        Note noteA;
        Note noteB;

        @BeforeEach
        void setup() {
            noteA = makeMe.aNote().please();
            noteB = makeMe.aNote().please();
        }

        @Nested
        class Related {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteA).linkTo(noteB, RELATED_TO).please();
            }


            @Test
            void AIsRelatedToB() {
                assertThat(noteA.linkTypes(), contains(RELATED_TO));
                assertThat(getLinkedNoteOfLinkType(LinkTypes.this.noteA, RELATED_TO), contains(noteB));
            }

            @Test
            void BIsAlsoRelatedToA() {
                assertThat(noteB.linkTypes(), contains(RELATED_TO));
                assertThat(getLinkedNoteOfLinkType(noteB, RELATED_TO), contains(noteA));
            }

        }

        @Nested
        class BelongsTo {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteA).linkTo(noteB, BELONGS_TO).please();
            }

            @Test
            void ABelongToB() {
                assertThat(noteA.linkTypes(), contains(BELONGS_TO));
                assertThat(getLinkedNoteOfLinkType(noteA, BELONGS_TO), contains(noteB));
            }

            @Test
            void BHasA() {
                assertThat(noteB.linkTypes(), contains(HAS));
                assertThat(getLinkedNoteOfLinkType(noteB, HAS), contains(noteA));
            }

        }

        @Nested
        class Has {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteA).linkTo(noteB, HAS).please();
            }

            @Test
            void AHasB() {
                assertThat(noteA.linkTypes(), contains(HAS));
                assertThat(getLinkedNoteOfLinkType(noteA, HAS), contains(noteB));
            }

            @Test
            void BBelongsToA() {
                assertThat(noteB.linkTypes(), contains(BELONGS_TO));
                assertThat(getLinkedNoteOfLinkType(noteB, BELONGS_TO), contains(noteA));
            }

        }

    }

    private List<Note> getLinkedNoteOfLinkType(Note noteA, Link.LinkType relatedTo) {
        List<Note> direct = noteA.linksOfTypeThroughDirect(relatedTo).stream().map(Link::getTargetNote).collect(toList());
        List<Note> reverse = noteA.linksOfTypeThroughReverse(relatedTo).stream().map(Link::getSourceNote).collect(Collectors.toUnmodifiableList());
        direct.addAll(reverse);
        return direct;
    }
}

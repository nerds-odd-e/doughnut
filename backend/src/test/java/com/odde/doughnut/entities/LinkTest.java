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
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.Link.LinkType.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class LinkTest {

    @Autowired MakeMe makeMe;
    User user;

    @Nested
    class LinkTypes {
        Note noteA;
        Note noteB;

        @BeforeEach
        void setup() {
            noteA = makeMe.aNote("noteA").please();
            noteB = makeMe.aNote("noteB").please();
        }

        @Test
        void allLinkTypesAreAvailable() {
            Link link = new Link();
            link.setSourceNote(noteA);
            link.setTargetNote(noteB);
            assertTrue(link.getPossibleLinkTypes().contains(RELATED_TO));
            assertTrue(link.getPossibleLinkTypes().contains(BELONGS_TO));
        }

        @Nested
        class Related {
            @BeforeEach
            void setup() {
                makeMe.theNote(noteA).linkTo(noteB, RELATED_TO).please();
            }


            @Test
            void AIsRelatedToB() {
                assertThat(noteA.linkTypes(null), contains(RELATED_TO));
                assertThat(getLinkedNoteOfLinkType(LinkTypes.this.noteA, RELATED_TO), contains(noteB));
            }

            @Test
            void BIsAlsoRelatedToA() {
                assertThat(noteB.linkTypes(null), contains(RELATED_TO));
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
                assertThat(noteA.linkTypes(null), contains(BELONGS_TO));
                assertThat(getLinkedNoteOfLinkType(noteA, BELONGS_TO), contains(noteB));
            }

            @Test
            void BHasA() {
                assertThat(noteB.linkTypes(null), contains(HAS));
                assertThat(getLinkedNoteOfLinkType(noteB, HAS), contains(noteA));
            }

            @Test
            void belongsIsNotAvailableBetweenTheSourceAndTargetAnyMore() {
                makeMe.refresh(noteA);
                Link link = new Link();
                link.setSourceNote(noteA);
                link.setTargetNote(noteB);
                assertTrue(link.getPossibleLinkTypes().contains(RELATED_TO));
                assertFalse(link.getPossibleLinkTypes().contains(BELONGS_TO));
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
                assertThat(noteA.linkTypes(null), contains(HAS));
                assertThat(getLinkedNoteOfLinkType(noteA, HAS), contains(noteB));
            }

            @Test
            void BBelongsToA() {
                assertThat(noteB.linkTypes(null), contains(BELONGS_TO));
                assertThat(getLinkedNoteOfLinkType(noteB, BELONGS_TO), contains(noteA));
            }

        }

    }

    private List<Note> getLinkedNoteOfLinkType(Note noteA, Link.LinkType relatedTo) {
        List<Note> direct = noteA.linksOfTypeThroughDirect(relatedTo).stream().map(Link::getTargetNote).collect(toList());
        List<Note> reverse = noteA.linksOfTypeThroughReverse(relatedTo, null).stream().map(Link::getSourceNote).collect(Collectors.toUnmodifiableList());
        direct.addAll(reverse);
        return direct;
    }
}

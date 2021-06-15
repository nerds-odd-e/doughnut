package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.LinkViewedByUser;
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
import java.util.Map;
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

    @Nested
    class LinkTypes {
        Note noteA;
        Note noteB;

        @BeforeEach
        void setup() {
            Note top = makeMe.aNote().please();
            noteA = makeMe.aNote("noteA").under(top).please();
            noteB = makeMe.aNote("noteB").under(top).please();
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
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteA.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(RELATED_TO));
                assertThat(getLinksOfBothDirection(RELATED_TO, allLinks), contains(noteB));
            }

            @Test
            void BIsAlsoRelatedToA() {
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteB.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(RELATED_TO));
                assertThat(getLinksOfBothDirection(RELATED_TO, allLinks), contains(noteA));
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
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteA.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(BELONGS_TO));
                assertThat(getLinksOfBothDirection(BELONGS_TO, allLinks), contains(noteB));
            }

            @Test
            void BHasA() {
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteB.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(HAS));
                assertThat(getLinksOfBothDirection(HAS, allLinks), contains(noteA));
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
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteA.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(HAS));
                assertThat(getLinksOfBothDirection(HAS, allLinks), contains(noteB));
            }

            @Test
            void BBelongsToA() {
                final Map<Link.LinkType, LinkViewedByUser> allLinks = noteB.getAllLinks(null);
                assertThat(allLinks.keySet(), contains(BELONGS_TO));
                assertThat(getLinksOfBothDirection(BELONGS_TO, allLinks), contains(noteA));
            }

        }

    }

    @Nested
    class ReverseLink {
        User user;
        Note target;

        @BeforeEach
        void setup() {
            user = makeMe.aUser().please();
            target = makeMe.aNote().byUser(user).please();

        }

        @Test
        void shouldGetReversedLinkIfItBelongsToTheUser() {
            Note source = makeMe.aNote().byUser(user).please();
            assertTrue(hasReverseLinkFor(source, user));
        }

        @Test
        void shouldNotGetReversedLinkIfItDoesNotBelongsToTheUser() {
            User anotherUser = makeMe.aUser().please();
            Note source = makeMe.aNote().byUser(anotherUser).please();
            assertFalse(hasReverseLinkFor(source, user));
        }

        private boolean hasReverseLinkFor(Note source, User viewer) {
            makeMe.theNote(source).linkTo(target, HAS).please();
            final LinkViewedByUser linksMap = target.getAllLinks(viewer).get(BELONGS_TO);
            if (linksMap == null) return false;
            final List<Link> links = linksMap.getReverse();
            return !links.isEmpty();
        }

    }

    private List<Note> getLinksOfBothDirection(Link.LinkType linkType, Map<Link.LinkType, LinkViewedByUser> allLinks) {
        List<Note> direct = allLinks.get(linkType).getDirect().stream().map(Link::getTargetNote).collect(toList());
        List<Note> reverse =allLinks.get(linkType).getReverse().stream().map(Link::getSourceNote).collect(Collectors.toUnmodifiableList());
        direct.addAll(reverse);
        return direct;
    }

}

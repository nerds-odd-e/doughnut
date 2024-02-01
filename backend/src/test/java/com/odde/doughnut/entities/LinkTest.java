package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.LinkType.RELATED_TO;
import static com.odde.doughnut.entities.LinkType.SPECIALIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.json.LinkViewed;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
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

    @Nested
    class Related {
      @BeforeEach
      void setup() {
        makeMe.theNote(noteA).linkTo(noteB, RELATED_TO).please();
      }

      @Test
      void AIsRelatedToB() {
        final Map<LinkType, LinkViewed> allLinks = new NoteViewer(null, noteA).getAllLinks();
        assertThat(allLinks.keySet(), contains(RELATED_TO));
        assertThat(getLinkedNotes(RELATED_TO, allLinks), contains(noteB));
      }
    }

    @Nested
    class BelongsTo {
      @BeforeEach
      void setup() {
        makeMe.theNote(noteA).linkTo(noteB, SPECIALIZE).please();
      }

      @Test
      void ABelongToB() {
        final Map<LinkType, LinkViewed> allLinks = new NoteViewer(null, noteA).getAllLinks();
        assertThat(allLinks.keySet(), contains(SPECIALIZE));
        assertThat(getLinkedNotes(SPECIALIZE, allLinks), contains(noteB));
      }

      @Test
      void BHasA() {
        final Map<LinkType, LinkViewed> allLinks = new NoteViewer(null, noteB).getAllLinks();
        assertThat(allLinks.keySet(), contains(SPECIALIZE));
        assertThat(allLinks.get(SPECIALIZE).getReverse(), hasSize(1));
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
      target = makeMe.aNote().creatorAndOwner(user).please();
    }

    @Test
    void shouldGetReversedLinkIfItBelongsToTheUser() {
      Note source = makeMe.aNote().creatorAndOwner(user).please();
      assertTrue(hasReverseLinkFor(source, user));
    }

    @Test
    void shouldNotGetReversedLinkIfItDoesNotBelongsToTheUser() {
      User anotherUser = makeMe.aUser().please();
      Note source = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertFalse(hasReverseLinkFor(source, user));
    }

    private boolean hasReverseLinkFor(Note source, User viewer) {
      makeMe.theNote(source).linkTo(target, SPECIALIZE).please();
      final LinkViewed linksMap = new NoteViewer(viewer, target).getAllLinks().get(SPECIALIZE);
      if (linksMap == null) return false;
      return !linksMap.getReverse().isEmpty();
    }
  }

  @Nested
  class LevelOfLink {
    Note source;
    Note target;

    @BeforeEach
    void setup() {
      source = makeMe.aNote().please();
      target = makeMe.aNote().please();
    }

    @Test
    void shouldGetSourceLevelWhenItIsHigher() {
      makeMe.theNote(source).level(5).please();
      Thing link = makeMe.aLink().between(source, target).inMemoryPlease();
      assertThat(link.getNote().getReviewSetting().getLevel(), is(5));
    }

    @Test
    void shouldGetTargetLevelWhenItIsHigher() {
      makeMe.theNote(target).level(5).please();
      Thing link = makeMe.aLink().between(source, target).inMemoryPlease();
      assertThat(link.getNote().getReviewSetting().getLevel(), is(5));
    }
  }

  private List<Note> getLinkedNotes(LinkType linkType, Map<LinkType, LinkViewed> allLinks) {
    return allLinks.get(linkType).getDirect().stream().map(Thing::getTargetNote).toList();
  }
}

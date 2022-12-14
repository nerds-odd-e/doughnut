package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.Link.LinkType.NO_LINK;
import static com.odde.doughnut.entities.Link.LinkType.RELATED_TO;
import static com.odde.doughnut.entities.Link.LinkType.SPECIALIZE;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
      Note top = makeMe.aHeadNote().please();
      noteA = makeMe.aNote("noteA").under(top).please();
      noteB = makeMe.aNote("noteB").under(top).please();
    }

    @Test
    void allLinkTypesAreAvailable() {
      Link link = Link.createLink(noteA, noteB, null, null, null);
      link.setSourceNote(noteA);
      link.setTargetNote(noteB);
      assertTrue(link.getPossibleLinkTypes().contains(RELATED_TO));
      assertTrue(link.getPossibleLinkTypes().contains(SPECIALIZE));
    }

    @Test
    public void shouldNeverUseTheNoLinkType() {
      Link link = makeMe.aLink().inMemoryPlease();
      link.setLinkType(NO_LINK);
      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      Set<ConstraintViolation<Link>> violations = factory.getValidator().validate(link);
      assertThat(violations, is(not(empty())));
      List<String> errorFields =
          violations.stream().map(v -> v.getPropertyPath().toString()).collect(toList());
      assertThat(errorFields, contains("linkType"));
    }

    @Nested
    class Related {
      @BeforeEach
      void setup() {
        makeMe.theNote(noteA).linkTo(noteB, RELATED_TO).please();
      }

      @Test
      void AIsRelatedToB() {
        final Map<Link.LinkType, LinkViewed> allLinks = new NoteViewer(null, noteA).getAllLinks();
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
        final Map<Link.LinkType, LinkViewed> allLinks = new NoteViewer(null, noteA).getAllLinks();
        assertThat(allLinks.keySet(), contains(SPECIALIZE));
        assertThat(getLinkedNotes(SPECIALIZE, allLinks), contains(noteB));
      }

      @Test
      void BHasA() {
        final Map<Link.LinkType, LinkViewed> allLinks = new NoteViewer(null, noteB).getAllLinks();
        assertThat(allLinks.keySet(), contains(SPECIALIZE));
        assertThat(allLinks.get(SPECIALIZE).getReverse(), hasSize(1));
      }

      @Test
      void belongsIsNotAvailableBetweenTheSourceAndTargetAnyMore() {
        makeMe.refresh(noteA);
        Link link = Link.createLink(noteA, noteB, null, null, null);
        link.setSourceNote(noteA);
        link.setTargetNote(noteB);
        assertTrue(link.getPossibleLinkTypes().contains(RELATED_TO));
        assertFalse(link.getPossibleLinkTypes().contains(SPECIALIZE));
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
      final List<Link> links = linksMap.getReverse();
      return !links.isEmpty();
    }
  }

  private List<Note> getLinkedNotes(
      Link.LinkType linkType, Map<Link.LinkType, LinkViewed> allLinks) {
    return allLinks.get(linkType).getDirect().stream().map(Link::getTargetNote).collect(toList());
  }
}

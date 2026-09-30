package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RelationControllerReduceIntoExistingKeyTests extends ControllerTestBase {
  @Autowired RelationController controller;
  @Autowired NoteReferenceService noteReferenceService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void aSingleValueBecomesAListAndEachTrackerFollowsItsValue()
      throws UnexpectedNoAccessRightException {
    Note sentence = sentenceWith("example of: \"[[run]]\"\n");
    MemoryTracker runTracker =
        makeMe
            .aMemoryTrackerFor(sentence)
            .by(currentUser.getUser())
            .propertyKey("example of")
            .please();
    Note relation = relationToPastTense(sentence);
    MemoryTracker relationTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).recallCount(1).please();
    float stability = relationTracker.getStability();

    controller.reduceToSourceProperty(relation);

    assertThat(
        sentence.getContent(), containsString("example of: [\"[[run]]\", \"[[past tense]]\"]"));
    assertThat(sentence.getContent(), not(containsString("example of 2")));
    assertThat(makeMe.refresh(runTracker).getPropertyValue(), equalTo("[[run]]"));
    assertThat(relationTracker.getNote().getId(), equalTo(sentence.getId()));
    assertThat(
        relationTracker.propertyFocus(),
        equalTo(new PropertyFocus("example of", "[[past tense]]")));
    assertThat(relationTracker.getRecallCount(), equalTo(1));
    assertThat(relationTracker.getStability(), equalTo(stability));
  }

  static Stream<Arguments> listsInEitherStyle() {
    return Stream.of(
        Arguments.of(
            "example of: [\"[[run]]\"]\n", "example of: [\"[[run]]\", \"[[past tense]]\"]\n"),
        Arguments.of(
            "example of:\n  - \"[[run]]\"\n",
            "example of:\n  - \"[[run]]\"\n  - \"[[past tense]]\"\n"));
  }

  @ParameterizedTest
  @MethodSource("listsInEitherStyle")
  void aValueIsAppendedToAKeyThatIsAlreadyAList(String authoredYaml, String expectedYaml)
      throws UnexpectedNoAccessRightException {
    Note sentence = sentenceWith(authoredYaml);
    Note relation = relationToPastTense(sentence);
    MemoryTracker relationTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).please();

    controller.reduceToSourceProperty(relation);

    assertThat(sentence.getContent(), equalTo("---\ntype: Note\n" + expectedYaml + "---\n"));
    assertThat(
        relationTracker.propertyFocus(),
        equalTo(new PropertyFocus("example of", "[[past tense]]")));
  }

  @Test
  void aValueAlreadyInTheListIsNotRepeatedAndTheLearnersExistingTrackerIsKept()
      throws UnexpectedNoAccessRightException {
    String authored = "example of: [\"[[run]]\", \"[[past tense]]\"]\n";
    Note sentence = sentenceWith(authored);
    MemoryTracker existing =
        makeMe
            .aMemoryTrackerFor(sentence)
            .by(currentUser.getUser())
            .propertyKey("example of")
            .propertyValue("[[past tense]]")
            .please();
    Note relation = relationToPastTense(sentence);
    makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).please();
    MemoryTracker otherLearners =
        makeMe.aMemoryTrackerFor(relation).by(makeMe.aUser().please()).please();

    controller.reduceToSourceProperty(relation);

    assertThat(sentence.getContent(), equalTo("---\ntype: Note\n" + authored + "---\n"));
    assertThat(
        memoryTrackerRepository.findByNote_IdIn(List.of(sentence.getId())),
        containsInAnyOrder(existing, otherLearners));
    assertThat(
        otherLearners.propertyFocus(), equalTo(new PropertyFocus("example of", "[[past tense]]")));
  }

  @Test
  void aKeyHoldingAMapIsNotOverwrittenAndTheReductionIsRefused() {
    Note sentence = sentenceWith("example of:\n  tense: \"[[run]]\"\n");
    String contentBefore = sentence.getContent();
    Note relation = relationToPastTense(sentence);
    MemoryTracker relationTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).please();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.reduceToSourceProperty(relation));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(exception.getReason(), containsString("example of"));
    assertThat(sentence.getContent(), equalTo(contentBefore));
    assertThat(relationTracker.getNote().getId(), equalTo(relation.getId()));
    assertThat(relationTracker.isNoteLevelTracker(), equalTo(true));
  }

  private Note sentenceWith(String yaml) {
    return makeMe
        .aNote("Sentence")
        .notebookOwnedBy(currentUser.getUser())
        .content("---\n" + yaml + "---\n")
        .please();
  }

  private Note relationToPastTense(Note sentence) {
    Note pastTense = makeMe.aNote("past tense").underSameNotebookAs(sentence).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(sentence)
            .asRelationship("an example of", sentence, pastTense)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    return relation;
  }
}

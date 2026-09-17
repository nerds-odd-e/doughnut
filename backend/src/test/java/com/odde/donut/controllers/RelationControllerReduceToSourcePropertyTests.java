package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerReduceToSourcePropertyTests extends ControllerTestBase {
  @Autowired RelationController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NoteReferenceService noteReferenceService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void reducesTheRelationshipIntoTheSourcePropertyAndPermanentlyDeletesTheRelationshipNote()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    Integer relationId = relation.getId();

    NoteRealm result = controller.reduceToSourceProperty(relation);

    assertThat(result.getNote().getId(), equalTo(source.getId()));
    assertThat(source.getContent(), containsString("a part of"));
    assertThat(source.getContent(), containsString("[[Earth]]"));
    assertThat(noteRepository.findById(relationId).isEmpty(), equalTo(true));
  }

  @Test
  void collidingKeySuffixesTheNewPropertyWhenTheSourceAlreadyHoldsIt()
      throws UnexpectedNoAccessRightException {
    Note source =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Somewhere]]\"\n---\n")
            .please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);

    controller.reduceToSourceProperty(relation);

    assertThat(source.getContent(), containsString("a part of 2"));
    assertThat(source.getContent(), containsString("[[Earth]]"));
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteTopology;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteContentAliasCardinalityTests
    extends TextContentControllerTestBase {
  @Autowired NoteController noteController;

  @Test
  void reresolvesNotebookShorthandsWhenAnAliasIntroducesOrRemovesACollision()
      throws UnexpectedNoAccessRightException {
    Note target =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("First")
            .aliases("Target")
            .please();
    Note carrier = makeMe.aNote().underSameNotebookAs(target).please();
    controller.updateNoteContent(carrier, contentDto("See [[Target]]."));
    InboundWiki inbound = new InboundWiki(target, carrier);
    Note namesake = makeMe.aNote().underSameNotebookAs(inbound.target()).title("Other").please();
    assertThat(
        noteController.showNote(inbound.carrier()).getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.updateNoteContent(
        namesake, contentDto("---\naliases:\n  - Target\n---\n\nBody text"));

    assertThat(
        noteController.showNote(inbound.carrier()).getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));

    controller.updateNoteContent(namesake, contentDto("Body text"));

    assertThat(
        noteController.showNote(inbound.carrier()).getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
  }

  @Test
  void newTitleTakesOverLiveResolutionAndInboundReferencesWithoutResavingSource()
      throws UnexpectedNoAccessRightException {
    Note alias =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("colour")
            .aliases("color")
            .please();
    Note source = makeMe.aNote().underSameNotebookAs(alias).please();
    String authored = "See [[color|Read this]].";
    String savedContent =
        controller.updateNoteContent(source, contentDto(authored)).getNote().getContent();
    assertThat(
        noteController.showNote(source).getWikiLinks().getFirst().getDestinationNoteId(),
        equalTo(alias.getId()));

    Note title = makeMe.aNote().underSameNotebookAs(alias).title("color").please();

    assertThat(
        noteController.showNote(source).getWikiLinks().getFirst().getDestinationNoteId(),
        equalTo(title.getId()));
    assertThat(
        noteController.showNote(title).getReferences().stream().map(NoteTopology::getId).toList(),
        contains(source.getId()));
    assertThat(noteController.showNote(source).getNote().getContent(), equalTo(savedContent));
  }
}

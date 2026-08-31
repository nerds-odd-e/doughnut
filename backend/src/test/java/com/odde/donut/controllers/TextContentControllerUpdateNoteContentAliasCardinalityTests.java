package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
    InboundWiki inbound = noteWithInboundWiki("Target", "See [[Target]].");
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
}

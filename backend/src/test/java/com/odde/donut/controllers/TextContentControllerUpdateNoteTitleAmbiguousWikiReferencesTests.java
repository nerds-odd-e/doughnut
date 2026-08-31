package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class TextContentControllerUpdateNoteTitleAmbiguousWikiReferencesTests
    extends TextContentControllerTestBase {

  @Test
  void keepVisibleText_lengthensRenamedPropertyLinkWhenNewTitleIsAmbiguous()
      throws UnexpectedNoAccessRightException {
    Folder solar = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("Solar").please();
    Note target =
        makeMe.aNote().title("Moon").folder(solar).content("---\na part of: Earth\n---\n").please();
    Folder mythology = makeMe.aFolder().notebook(solar.getNotebook()).name("Mythology").please();
    makeMe.aNote().title("Luna").folder(mythology).please();
    Note carrier = makeMe.aNote().notebook(solar.getNotebook()).please();
    controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of|the moon]]"));

    NoteUpdateTitleDTO titleDto = titleDto("Luna");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

    controller.updateNoteTitle(target, titleDto);

    makeMe.refresh(carrier);
    assertThat(carrier.getContent(), containsString("[[Solar/Luna#prop:a%20part%20of|the moon]]"));
  }

  @Test
  void updateVisibleText_keepsPathMarkdownSpellingWhenRenamedTitleIsAmbiguous()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound =
        folderPathInboundWiki("See [one](/Folder/Old.md) and [two](/Folder/Old).");
    Folder namesakeFolder =
        makeMe.aFolder().notebook(inbound.target().getNotebook()).name("Namesake").please();
    makeMe.aNote().title("New").folder(namesakeFolder).please();

    NoteUpdateTitleDTO titleDto = titleDto("New");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    controller.updateNoteTitle(inbound.target(), titleDto);

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[one](/Folder/New.md)"));
    assertThat(inbound.carrier().getContent(), containsString("[two](/Folder/New)"));
    assertThat(inbound.carrier().getContent(), not(containsString("[[")));
  }
}

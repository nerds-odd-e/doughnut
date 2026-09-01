package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerNoteIdUrlTests extends TextContentControllerTestBase {

  @Autowired ResolvedWikiLinkRepository resolvedWikiLinkRepository;

  @Test
  void rootRelativeNoteUrlWithWrongDisplayText_indexesSemanticReference()
      throws UnexpectedNoAccessRightException {
    Note target =
        makeMe.aNote().title("Real Title").notebookOwnedBy(currentUser.getUser()).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    String markdown = "[wrong display](/n" + target.getId() + ")";

    NoteRealm response = controller.updateNoteContent(source, contentDto(markdown));

    assertThat(response.getWikiLinks(), hasSize(1));
    WikiLink wt = response.getWikiLinks().getFirst();
    assertThat(wt.getAuthoredLink(), equalTo(markdown));
    assertThat(wt.getTarget(), equalTo("/n" + target.getId()));
    assertThat(wt.getDisplayText(), equalTo("wrong display"));
    assertThat(wt.getDestinationNoteId(), equalTo(target.getId()));
    assertThat(response.getNote().getContent(), containsString(markdown));

    List<ResolvedWikiLink> rows =
        resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(source.getId());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getDestinationNote().getId(), equalTo(target.getId()));
    assertThat(rows.getFirst().getAuthoredLink(), equalTo(markdown));
  }

  @Test
  void absoluteCanonicalNoteUrl_indexesSemanticReference() throws UnexpectedNoAccessRightException {
    Note target =
        makeMe.aNote().title("Real Title").notebookOwnedBy(currentUser.getUser()).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    String href = "https://donut.test/n" + target.getId();
    String markdown = "[wrong display](" + href + ")";

    NoteRealm response = controller.updateNoteContent(source, contentDto(markdown));

    assertThat(response.getWikiLinks(), hasSize(1));
    WikiLink wt = response.getWikiLinks().getFirst();
    assertThat(wt.getAuthoredLink(), equalTo(markdown));
    assertThat(wt.getTarget(), equalTo(href));
    assertThat(wt.getDisplayText(), equalTo("wrong display"));
    assertThat(wt.getDestinationNoteId(), equalTo(target.getId()));
    assertThat(response.getNote().getContent(), containsString(markdown));

    List<ResolvedWikiLink> rows =
        resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(source.getId());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getDestinationNote().getId(), equalTo(target.getId()));
    assertThat(rows.getFirst().getAuthoredLink(), equalTo(markdown));
  }

  @Test
  void foreignOriginNoteLookingUrl_staysOrdinaryMarkdown() throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote().title("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    String markdown = "[label](https://evil.example/n" + target.getId() + ")";

    NoteRealm response = controller.updateNoteContent(source, contentDto(markdown));

    assertThat(response.getWikiLinks(), empty());
    assertThat(resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(source.getId()), empty());
    assertThat(response.getNote().getContent(), containsString(markdown));
  }

  @Test
  void missingNoteIdUrl_staysOrdinaryMarkdown() throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

    NoteRealm response = controller.updateNoteContent(source, contentDto("[gone](/n99999999)"));

    assertThat(response.getWikiLinks(), empty());
    assertThat(resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(source.getId()), empty());
    assertThat(response.getNote().getContent(), containsString("[gone](/n99999999)"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/n/%d", "/n%d/p/topic", "/n%d?x=1", "/n%d#frag"})
  void nonCanonicalNoteUrlForms_areNotIndexedAsReferences(String hrefTemplate)
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote().title("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    String href = hrefTemplate.formatted(target.getId());

    NoteRealm response = controller.updateNoteContent(source, contentDto("[label](" + href + ")"));

    assertThat(response.getWikiLinks(), empty());
    assertThat(resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(source.getId()), empty());
  }
}

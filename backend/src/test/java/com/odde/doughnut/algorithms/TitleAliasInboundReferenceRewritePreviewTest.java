package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import org.junit.jupiter.api.Test;

class TitleAliasInboundReferenceRewritePreviewTest {

  @Test
  void previewRow_bareLegacyTitleLinkPlansPrimaryTitleRewrite() {
    Note target = noteWithTitle("colour");
    Note referrer = noteWithTitle("referrer");
    NoteWikiTitleCache row = cacheRow(referrer, target, "colour／color");

    var preview = TitleAliasInboundReferenceRewritePreview.previewRow(row).orElseThrow();

    assertThat(preview.plannedLinkInner(), equalTo("colour"));
    assertThat(preview.visibleTextWillChange(), is(true));
  }

  @Test
  void previewRow_qualifiedLegacyTitleLinkKeepsNotebookPrefix() {
    Note target = noteWithTitle("colour");
    Note referrer = noteWithTitle("referrer");
    NoteWikiTitleCache row = cacheRow(referrer, target, "MyNb:colour／color");

    var preview = TitleAliasInboundReferenceRewritePreview.previewRow(row).orElseThrow();

    assertThat(preview.plannedLinkInner(), equalTo("MyNb:colour"));
    assertThat(preview.visibleTextWillChange(), is(true));
  }

  @Test
  void previewRow_pipedLegacyTitleLinkPreservesDisplayText() {
    Note target = noteWithTitle("colour");
    Note referrer = noteWithTitle("referrer");
    NoteWikiTitleCache row = cacheRow(referrer, target, "colour／color|custom label");

    var preview = TitleAliasInboundReferenceRewritePreview.previewRow(row).orElseThrow();

    assertThat(preview.plannedLinkInner(), equalTo("colour|custom label"));
    assertThat(preview.visibleTextWillChange(), is(false));
  }

  @Test
  void previewRow_skipsAlreadyPrimaryTitleLinks() {
    Note target = noteWithTitle("colour");
    Note referrer = noteWithTitle("referrer");
    NoteWikiTitleCache row = cacheRow(referrer, target, "colour");

    assertThat(TitleAliasInboundReferenceRewritePreview.previewRow(row).isEmpty(), is(true));
  }

  private static Note noteWithTitle(String title) {
    Note note = mock(Note.class);
    when(note.getTitle()).thenReturn(title);
    when(note.getDeletedAt()).thenReturn(null);
    when(note.getId()).thenReturn(1);
    return note;
  }

  private static NoteWikiTitleCache cacheRow(Note referrer, Note target, String linkText) {
    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(referrer);
    row.setTargetNote(target);
    row.setLinkText(linkText);
    return row;
  }
}

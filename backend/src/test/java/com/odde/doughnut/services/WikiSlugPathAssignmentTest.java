package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WikiSlugPathAssignmentTest {

  @Test
  void noteSlugFitsMaxLengthWhenFolderPrefixConsumesMostOfLimit() {
    Note note = new Note();
    Folder folder = new Folder();
    folder.setSlug("p".repeat(760));
    note.setFolder(folder);
    note.setTitle("Untitled");
    WikiSlugPathAssignment.setNoteSlug(note, Set.of("untitled"));
    assertThat(note.getSlug().length(), lessThanOrEqualTo(Note.MAX_SLUG_LENGTH));
    assertThat(note.getSlug().startsWith(folder.getSlug() + "/"), equalTo(true));
  }
}

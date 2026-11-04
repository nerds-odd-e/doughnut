package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** A web save dates the property values it adds or changes at the commit it appends. */
class NotebookGitWebContentPropertyDatingControllerTest
    extends NotebookGitWebContentControllerTestBase {
  private static final Timestamp CREATED = Timestamp.from(Instant.parse("2026-09-01T08:00:00Z"));
  private static final Timestamp T1 = Timestamp.from(Instant.parse("2026-09-08T10:00:00Z"));
  private static final Timestamp T2 = Timestamp.from(Instant.parse("2026-09-08T10:08:00Z"));

  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;

  private Notebook notebook;
  private Note note;

  @BeforeEach
  void gitBackedNote() throws Exception {
    notebook = createGitBackedNotebook();
    note = noteTitled("Atom");
    snapshotCurrentPortableTree(notebook);
  }

  @Test
  void anAddedValueIsDatedAtTheCommitTheSaveAppends() throws Exception {
    saveAt(note, "---\ntopic: physics\n---\nbody", T1);

    assertThat(changedAt(note, "topic", ""), equalTo(T1));
    assertThat(acceptedTipCommitTime(), equalTo(T1.toInstant()));
  }

  @Test
  void aBodyOnlySaveKeepsTheValueDate() throws Exception {
    saveAt(note, "---\ntopic: physics\n---\nbody", T1);

    saveAt(note, "---\ntopic: physics\n---\nedited body", T2);

    assertThat(changedAt(note, "topic", ""), equalTo(T1));
  }

  @Test
  void addingAnotherPropertyKeepsTheExistingValueDate() throws Exception {
    saveAt(note, "---\ntopic: physics\n---\nbody", T1);

    saveAt(note, "---\ntopic: physics\norigin: lab\n---\nbody", T2);

    assertThat(changedAt(note, "topic", ""), equalTo(T1));
    assertThat(changedAt(note, "origin", ""), equalTo(T2));
  }

  @Test
  void reorderingAListKeepsItsItemDatesAndDatesAnAddedItem() throws Exception {
    saveAt(note, "---\ncolors: [red, blue]\n---\nbody", T1);

    saveAt(note, "---\ncolors: [blue, red, green]\n---\nbody", T2);

    assertThat(changedAt(note, "colors", "red"), equalTo(T1));
    assertThat(changedAt(note, "colors", "blue"), equalTo(T1));
    assertThat(changedAt(note, "colors", "green"), equalTo(T2));
  }

  @Test
  void editingAScalarRedatesIt() throws Exception {
    saveAt(note, "---\ndefinition: foo\n---\nbody", T1);

    saveAt(note, "---\ndefinition: bar\n---\nbody", T2);

    assertThat(changedAt(note, "definition", ""), equalTo(T2));
  }

  @Test
  void savingOneNoteLeavesAnotherNotesValueDates() throws Exception {
    Note other = noteTitled("Molecule");
    snapshotCurrentPortableTree(notebook);
    saveAt(other, "---\ntopic: chemistry\n---\nbody", T1);

    saveAt(note, "---\ntopic: physics\n---\nbody", T2);

    assertThat(changedAt(other, "topic", ""), equalTo(T1));
  }

  @Test
  void aRenameThatRewritesALinkValueDatesItAtTheRenameCommit() throws Exception {
    Note referrer = noteTitled("Electron");
    snapshotCurrentPortableTree(notebook);
    saveAt(referrer, "---\nexample of: \"[[Atom]]\"\n---\nbody", T1);
    NoteUpdateTitleDTO rename = titleDto("Nucleus");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);
    testabilitySettings.timeTravelTo(T2);

    textContentController.updateNoteTitle(reload(note), rename);

    assertThat(changedAt(referrer, "example of", ""), equalTo(T2));
  }

  private Note noteTitled(String title) {
    return makeMe.aNote().notebook(notebook).title(title).createdAt(CREATED).please();
  }

  private void saveAt(Note target, String content, Timestamp at) throws Exception {
    testabilitySettings.timeTravelTo(at);
    textContentController.updateNoteContent(reload(target), contentDto(content));
  }

  private Note reload(Note target) {
    return noteRepository.findById(target.getId()).orElseThrow();
  }

  private Timestamp changedAt(Note target, String key, String value) {
    return notePropertyIndexRepository
        .findByNote_IdAndPropertyKeyAndPropertyValue(target.getId(), key, value)
        .getFirst()
        .getChangedAt();
  }

  private Instant acceptedTipCommitTime() throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(repository)) {
      return walk.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getCommitterIdent()
          .getWhenAsInstant();
    }
  }
}

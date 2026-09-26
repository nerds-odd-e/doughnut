package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteImageUploadDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class NoteControllerUploadNoteImageTests extends NotebookGitWebContentControllerTestBase {
  @Test
  void theUploadedPictureIsAFileInTheNotesFolderNamedByItsImageInOneAcceptedCommit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note moon = makeMe.aNote("Moon").folder(physics).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();
    MultipartFile picture = makeMe.anUploadedImage().toMultiplePartFilePlease();

    upload(moon, picture);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(
        tipContent(after, "physics/my.png"),
        equalTo(lfsPointerStoredFor(notebook, picture.getBytes())));
    assertThat(
        tipText(after, "physics/Moon.md"),
        equalTo("---\ntype: Note\nimage: my.png\n---\naccepted content"));
  }

  @Test
  void aRootNotesPictureIsAFileAtTheNotebookRootAndItsContentIsPreparedLikeAnySave()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moon = makeMe.aNote("Moon").notebook(notebook).content("no frontmatter").please();
    snapshotCurrentPortableTree(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();
    MultipartFile picture = makeMe.anUploadedImage().toMultiplePartFilePlease();

    NoteRealm realm = upload(moon, picture);

    String prepared = "---\ntype: Note\nimage: my.png\n---\nno frontmatter";
    assertThat(realm.getNote().getContent(), equalTo(prepared));
    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(
        tipContent(after, "my.png"), equalTo(lfsPointerStoredFor(notebook, picture.getBytes())));
    assertThat(tipText(after, "Moon.md"), equalTo(prepared));
  }

  @Test
  void replacingAPictureChangesOnlyItsImageLine() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    String authored =
        "---\ntags: [x, y]\n# a comment\ndescription: \"Quoted: value\"\n"
            + "image: earlier.png\nimage_mask: 10 10 20 20\ntype: Note\n---\nbody";
    Note moon = makeMe.aNote("Moon").folder(physics).content(authored).please();
    storeFolderAttachmentAndSnapshot(notebook, physics, "earlier.png", "earlier".getBytes());

    upload(moon, makeMe.anUploadedImage().toMultiplePartFilePlease());

    assertThat(
        tipText(acceptedHistory(notebook), "physics/Moon.md"),
        equalTo(authored.replace("image: earlier.png", "image: my.png")));
  }

  @Test
  void aNameTakenInTheNotesFolderIsRefusedAndNothingChanges() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, physics, "diagram.png", "earlier".getBytes());

    assertUploadRefusedWithNothingChanged(force, "diagram.png", "physics/diagram.png");
  }

  @Test
  void aNameTakenInTheNotesFolderIgnoringCaseIsRefusedAndNothingChanges() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, physics, "Force.png", "earlier".getBytes());

    assertUploadRefusedWithNothingChanged(force, "force.png", "physics/force.png");
  }

  @Test
  void aNameUsedByANoteInTheNotesFolderIsRefusedAndNothingChanges() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    assertUploadRefusedWithNothingChanged(force, "force.md", "physics/force.md");
  }

  @Test
  void aNameUsedByAFolderInTheNotesFolderIsRefusedAndNothingChanges() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder sub = makeMe.aFolder().parentFolder(physics).name("sub").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    makeMe.aNote("inside").folder(sub).please();
    snapshotCurrentPortableTree(notebook);

    assertUploadRefusedWithNothingChanged(force, "sub", "physics/sub");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", ".", "..", "a/b.png", ".keep.png"})
  void aNameThatIsNotAPlainFilenameIsRefusedAndNothingChanges(String name) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    assertUploadRefusedWithNothingChanged(force, name, "physics/" + name);
  }

  private void assertUploadRefusedWithNothingChanged(Note note, String name, String path)
      throws Exception {
    assertUploadRefusedWithNothingChanged(
        note, makeMe.anUploadedImage().originalFilename(name).toMultiplePartFilePlease(), path);
  }

  private void assertUploadRefusedWithNothingChanged(
      Note note, MultipartFile picture, String messagePart) throws Exception {
    Integer notebookId = note.getNotebook().getId();
    List<String> commitsBefore = acceptedHistory(note.getNotebook()).commits();
    long attachmentsBefore = notebookAttachmentRepository.count();

    ApiException refusal = assertThrows(ApiException.class, () -> upload(note, picture));

    assertThat(refusal.getErrorBody().getMessage(), containsString(messagePart));
    assertThat(acceptedHistory(note.getNotebook()).commits(), equalTo(commitsBefore));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(ACCEPTED_CONTENT));
    assertThat(notebookAttachmentRepository.count(), equalTo(attachmentsBefore));
    assertThat(
        notebookAttachmentContent.get(
            notebookId, VerifiedNotebookAttachmentBytes.sha256Hex(picture.getBytes())),
        equalTo(Optional.empty()));
  }

  @Nested
  class AdmittedByNameUpToTheLimit {
    private static final int LIMIT = 10 * 1024 * 1024;
    private Note force;

    @BeforeEach
    void setup() throws Exception {
      Notebook notebook = createGitBackedNotebook();
      Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
      force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
      snapshotCurrentPortableTree(notebook);
    }

    @ParameterizedTest
    @ValueSource(strings = {"notes.txt", "drawing.svg"})
    void aNameThatIsNotAPictureIsRefusedNamingTheAllowedTypes(String name) throws Exception {
      assertUploadRefusedWithNothingChanged(force, name, "png, jpg, jpeg, gif or webp");
    }

    @Test
    void aPictureOverTheLimitIsRefused() throws Exception {
      assertUploadRefusedWithNothingChanged(
          force,
          makeMe.anUploadedImage().bytes(new byte[LIMIT + 1]).toMultiplePartFilePlease(),
          "File size exceeds the limit: 10485760 bytes.");
    }

    @Test
    void aPictureAtTheLimitIsAccepted() throws Exception {
      NoteRealm realm =
          upload(force, makeMe.anUploadedImage().bytes(new byte[LIMIT]).toMultiplePartFilePlease());

      assertThat(realm.getNote().getContent(), containsString("image: my.png"));
    }

    @Test
    void theDeclaredContentTypeAndTheBytesAreNotChecked() throws Exception {
      NoteRealm realm =
          upload(
              force,
              makeMe
                  .anUploadedImage()
                  .originalFilename("photo.png")
                  .contentType("application/octet-stream")
                  .bytes("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes())
                  .toMultiplePartFilePlease());

      assertThat(realm.getNote().getContent(), containsString("image: photo.png"));
    }
  }

  @Test
  void aNotebookWithoutAGitBindingRefusesTheUploadAndStoresNothing() {
    Note moon = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    long attachmentsBefore = notebookAttachmentRepository.count();
    MultipartFile picture = makeMe.anUploadedImage().toMultiplePartFilePlease();

    ResponseStatusException refusal =
        assertThrows(ResponseStatusException.class, () -> upload(moon, picture));

    assertThat(refusal.getReason(), equalTo("Notebook has no Git binding."));
    assertThat(notebookAttachmentRepository.count(), equalTo(attachmentsBefore));
    assertThat(
        noteRepository.findById(moon.getId()).orElseThrow().getContent(),
        equalTo(moon.getContent()));
  }

  @Test
  void shouldNotAllowUploadForNoteBelongingToAnotherUser() {
    Note note = makeMe.aNote().notebookOwnedBy(createFixtureUser()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> noteController.uploadNoteImage(note, new NoteImageUploadDTO()));
  }

  private NoteRealm upload(Note note, MultipartFile picture) throws Exception {
    NoteImageUploadDTO dto = new NoteImageUploadDTO();
    dto.setUploadImage(picture);
    return noteController.uploadNoteImage(noteRepository.findById(note.getId()).orElseThrow(), dto);
  }
}

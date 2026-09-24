package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteImageUploadDTO;
import com.odde.donut.controllers.dto.NoteImageUploadResult;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class NoteControllerUploadNoteImageTests extends NotebookGitWebContentControllerTestBase {
  @Autowired NoteAttachmentImageController noteAttachmentImageController;

  @Test
  void theUploadedPictureIsAFileInTheNotesFolderNamedByItsImageInOneAcceptedCommit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note moon = makeMe.aNote("Moon").folder(physics).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();
    MultipartFile picture = makeMe.anUploadedImage().toMultiplePartFilePlease();

    NoteImageUploadResult result = upload(moon, picture);

    assertThat(result.imagePath(), equalTo("my.png"));
    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(
        tipContent(after, "physics/my.png"), equalTo(lfsPointerStoredFor(notebook, picture)));
    assertThat(
        tipText(after, "physics/Moon.md"),
        equalTo("---\ntype: Note\nimage: my.png\n---\naccepted content"));
    assertThat(legacyImageCount(moon), equalTo(0L));
  }

  @Test
  void aRootNotesPictureIsAFileAtTheNotebookRoot() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moon = makeMe.aNote("Moon").notebook(notebook).content("no frontmatter").please();
    snapshotCurrentPortableTree(notebook);
    MultipartFile picture = makeMe.anUploadedImage().toMultiplePartFilePlease();

    upload(moon, picture);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(tipContent(after, "my.png"), equalTo(lfsPointerStoredFor(notebook, picture)));
    assertThat(tipText(after, "Moon.md"), equalTo("---\nimage: my.png\n---\nno frontmatter"));
  }

  @Test
  void replacingALegacyPictureKeepsItsMaskAndItsRow() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moon = makeMe.aNote("Moon").notebook(notebook).please();
    Image legacy = makeMe.anImage().forNote(moon).by(currentUser.getUser()).please();
    authorReferencingContentCommitted(
        moon,
        "---\nimage: /attachments/images/"
            + legacy.getId()
            + "/example.png\nimage_mask: 10 10 20 20\n---\nbody");
    snapshotCurrentPortableTree(notebook);

    upload(moon, makeMe.anUploadedImage().toMultiplePartFilePlease());

    assertThat(
        tipText(acceptedHistory(notebook), "Moon.md"),
        equalTo("---\nimage: my.png\nimage_mask: 10 10 20 20\n---\nbody"));
    assertThat(legacyImageCount(moon), equalTo(1L));
  }

  @Test
  void shouldKeepTheOriginalBytesOfAPictureWiderThan2000Pixels() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moon = makeMe.aNote("Moon").notebook(notebook).please();
    MultipartFile picture = makeMe.anUploadedImage().metrics(2001, 2).toMultiplePartFilePlease();

    upload(moon, picture);

    assertThat(
        noteAttachmentImageController
            .showAttachmentImage(noteRepository.findById(moon.getId()).orElseThrow(), "my.png")
            .getBody(),
        equalTo(picture.getBytes()));
  }

  @Test
  void aNameTakenInTheNotesFolderIsRefusedAndNothingChanges() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, physics, "diagram.png", "earlier".getBytes());

    assertUploadRefusedWithNothingChanged(force, "diagram.png", "physics/diagram.png");
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
    Integer notebookId = note.getNotebook().getId();
    List<String> commitsBefore = acceptedHistory(note.getNotebook()).commits();
    long attachmentsBefore = notebookAttachmentRepository.count();
    MultipartFile picture =
        makeMe.anUploadedImage().originalFilename(name).toMultiplePartFilePlease();

    ApiException refusal = assertThrows(ApiException.class, () -> upload(note, picture));

    assertThat(refusal.getErrorBody().getMessage(), containsString(path));
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

  @Test
  void aNotebookWithoutLfsFilesRefusesTheUploadLoudly() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    Note moon = makeMe.aNote("Moon").notebook(notebook).please();
    List<String> commitsBefore = acceptedHistory(notebook).commits();

    assertThrows(
        IllegalStateException.class,
        () -> upload(moon, makeMe.anUploadedImage().toMultiplePartFilePlease()));

    assertThat(acceptedHistory(notebook).commits(), equalTo(commitsBefore));
  }

  @Test
  void shouldNotAllowUploadForNoteBelongingToAnotherUser() {
    Note note = makeMe.aNote().notebookOwnedBy(createFixtureUser()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> noteController.uploadNoteImage(note, new NoteImageUploadDTO()));
  }

  @Test
  void shouldRejectInvalidUploadContentType() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile("uploadImage", "x.pdf", "application/pdf", "not-empty".getBytes()));
      assertThat(factory.getValidator().validate(dto), is(not(empty())));
    }
  }

  @Test
  void shouldAcceptExactLimitImageUploadDtoThroughBeanValidation() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile(
              "uploadImage", "exact.png", "image/png", new byte[10 * 1024 * 1024]));
      assertThat(factory.getValidator().validate(dto), is(empty()));
    }
  }

  @Test
  void shouldRejectOneByteOverImageUploadDtoThroughBeanValidation() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile(
              "uploadImage", "over.png", "image/png", new byte[10 * 1024 * 1024 + 1]));
      assertThat(factory.getValidator().validate(dto), is(not(empty())));
    }
  }

  private NoteImageUploadResult upload(Note note, MultipartFile picture) throws Exception {
    NoteImageUploadDTO dto = new NoteImageUploadDTO();
    dto.setUploadImage(picture);
    return noteController.uploadNoteImage(noteRepository.findById(note.getId()).orElseThrow(), dto);
  }

  /** The pointer for {@code picture}, after checking the content store holds its exact bytes. */
  private byte[] lfsPointerStoredFor(Notebook notebook, MultipartFile picture) throws Exception {
    byte[] bytes = picture.getBytes();
    String digest = VerifiedNotebookAttachmentBytes.sha256Hex(bytes);
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), digest).orElseThrow(), equalTo(bytes));
    return NotebookGitLfsPointer.format(digest, bytes.length);
  }

  private static byte[] tipContent(AcceptedHistory history, String path) {
    return history.content().stream()
        .filter(entry -> entry.path().equals(path))
        .map(PortableTreeEntry::content)
        .findFirst()
        .orElseThrow(() -> new AssertionError(path + " not in " + history.tipPaths()));
  }

  private static String tipText(AcceptedHistory history, String path) {
    return new String(tipContent(history, path), StandardCharsets.UTF_8);
  }

  private long legacyImageCount(Note note) {
    return entityManager
        .createQuery("SELECT COUNT(i) FROM Image i WHERE i.note.id = :noteId", Long.class)
        .setParameter("noteId", note.getId())
        .getSingleResult();
  }
}

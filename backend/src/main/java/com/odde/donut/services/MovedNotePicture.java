package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NoteFolderAttachment;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * The picture a note moved to another folder within its notebook takes along: the file its {@code
 * image:} names as a plain filename in its own folder. The file moves with the note, or the note
 * gets a copy reusing the same stored bytes while another note in the source folder also names it.
 * In the new folder it takes the first name no entry there holds (ignoring case), and the note's
 * {@code image:} is rewritten when that name differs.
 */
@Service
public class MovedNotePicture {
  private final NoteFolderAttachment noteFolderAttachment;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public MovedNotePicture(
      NoteFolderAttachment noteFolderAttachment,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.noteFolderAttachment = noteFolderAttachment;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  /**
   * Runs {@code placeNote}, which places the note in {@code destinationOrNull}, carrying its
   * picture.
   */
  public void placeWithPicture(
      Note note, Folder destinationOrNull, Timestamp now, Runnable placeNote) {
    Optional<NotebookAttachment> picture = pictureToCarry(note, destinationOrNull);
    placeNote.run();
    picture.ifPresent(file -> carry(note, file, destinationOrNull, now));
  }

  private Optional<NotebookAttachment> pictureToCarry(Note note, Folder destinationOrNull) {
    if (Objects.equals(folderId(note.getFolder()), folderId(destinationOrNull))) {
      return Optional.empty();
    }
    return NoteContentMarkdown.noteImage(note.getContent())
        .filter(NotebookGitPortablePath::isPlainFilename)
        .flatMap(image -> noteFolderAttachment.at(note, image))
        .map(file -> anotherNoteNames(note, file.getFilename()) ? copyOf(file) : file);
  }

  private boolean anotherNoteNames(Note note, String filename) {
    List<Note> sourceFolderNotes =
        note.getFolder() == null
            ? noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
                note.getNotebook().getId())
            : noteRepository.findNotesInFolderOrderByIdAsc(note.getFolder().getId());
    return sourceFolderNotes.stream()
        .filter(other -> !other.getId().equals(note.getId()))
        .anyMatch(
            other ->
                NoteContentMarkdown.noteImage(other.getContent()).equals(Optional.of(filename)));
  }

  private static NotebookAttachment copyOf(NotebookAttachment file) {
    NotebookAttachment copy = new NotebookAttachment();
    copy.setNotebook(file.getNotebook());
    copy.setFilename(file.getFilename());
    copy.setAcceptedGitContent(file.getAcceptedGitContent());
    return copy;
  }

  private void carry(Note note, NotebookAttachment file, Folder destinationOrNull, Timestamp now) {
    String filename =
        NumberedNameSelection.firstAvailableFilename(
            file.getFilename(),
            candidate ->
                folderSiblingNameValidation
                    .entryHolding(note.getNotebook(), destinationOrNull, candidate, Set.of())
                    .isPresent());
    if (!filename.equals(file.getFilename())) {
      file.setFilename(filename);
      authoredNoteDocumentPersistence.persist(
          note,
          AuthoredNoteDocument.fromContent(
              NoteContentMarkdown.withNoteImage(note.getContent(), filename), canonicalDonutOrigin),
          now);
    }
    file.setFolder(destinationOrNull);
    entityPersister.merge(file);
  }

  private static Integer folderId(Folder folderOrNull) {
    return folderOrNull == null ? null : folderOrNull.getId();
  }
}

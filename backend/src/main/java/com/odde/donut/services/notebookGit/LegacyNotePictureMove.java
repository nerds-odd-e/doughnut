package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteContentMarkdown.LeadingFrontmatterImageReference.Referenced;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A note whose {@code image:} names a legacy upload owned by a note in the same notebook gets that
 * picture as a file in its own folder, and {@code image:} names the file. The pictures' bytes are
 * stored first, then all of a notebook's moved pictures are accepted as one web change. The legacy
 * rows stay as the backup.
 */
@Service
public class LegacyNotePictureMove {
  private final EntityPersister entityPersister;
  private final NotebookAttachmentContent attachmentContent;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NoteImageFileAttachment noteImageFileAttachment;
  private final TestabilitySettings testabilitySettings;

  public LegacyNotePictureMove(
      EntityPersister entityPersister,
      NotebookAttachmentContent attachmentContent,
      AcceptedWebChangeService acceptedWebChangeService,
      NoteImageFileAttachment noteImageFileAttachment,
      TestabilitySettings testabilitySettings) {
    this.entityPersister = entityPersister;
    this.attachmentContent = attachmentContent;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.noteImageFileAttachment = noteImageFileAttachment;
    this.testabilitySettings = testabilitySettings;
  }

  @Transactional(rollbackFor = Exception.class)
  public void move(Integer notebookId) throws IOException, UnexpectedNoAccessRightException {
    Map<Note, Image> pictures = ownLegacyPictures(notebookId);
    if (pictures.isEmpty()) {
      return;
    }
    Map<Image, byte[]> pointers = new HashMap<>();
    for (Image image : pictures.values()) {
      if (!pointers.containsKey(image)) {
        pointers.put(
            image, attachmentContent.storeAsLfsPointer(notebookId, image.getBlob().getData()));
      }
    }
    acceptedWebChangeService.apply(
        notebookId,
        () -> {
          pictures.forEach(
              (note, image) -> {
                if (legacyImageId(note).equals(Optional.of(image.getId()))) {
                  noteImageFileAttachment.attach(
                      note, image.getName(), pointers.get(image), note.getUpdatedAt());
                }
              });
          return null;
        },
        ignored -> "Move uploaded pictures into the notebook",
        testabilitySettings.getCurrentUTCTimestamp());
  }

  private Map<Note, Image> ownLegacyPictures(Integer notebookId) {
    List<Note> notes =
        entityPersister
            .createQuery(
                "FROM Note n WHERE n.notebook.id = :notebookId"
                    + " AND n.content LIKE '%/attachments/images/%' ORDER BY n.id",
                Note.class)
            .setParameter("notebookId", notebookId)
            .getResultList();
    Map<Note, Image> pictures = new LinkedHashMap<>();
    for (Note note : notes) {
      legacyImageId(note)
          .map(id -> entityPersister.find(Image.class, id))
          .filter(image -> image.getNote().getNotebook().getId().equals(notebookId))
          .ifPresent(image -> pictures.put(note, image));
    }
    return pictures;
  }

  private static Optional<Integer> legacyImageId(Note note) {
    return NoteContentMarkdown.leadingFrontmatterImageReference(note.getContent())
            instanceof Referenced referenced
        ? Optional.of(referenced.imageId())
        : Optional.empty();
  }
}

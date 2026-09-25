package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteContentMarkdown.LeadingFrontmatterImageReference.Referenced;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.NumberedNameSelection;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A note whose {@code image:} names a legacy upload owned by a note in the same notebook gets that
 * picture as a file in its own folder under the next free name, and {@code image:} names the file.
 * The pictures' bytes are stored first, then all of a notebook's moved pictures are accepted as one
 * web change. The legacy rows stay as the backup. A reference to an upload not owned in this
 * notebook (another notebook's, or a row without a note) is left unchanged and counted; a reference
 * to a missing row is left unchanged.
 */
@Service
public class LegacyNotePictureMove {
  private final EntityPersister entityPersister;
  private final NotebookAttachmentContent attachmentContent;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NoteImageFileAttachment noteImageFileAttachment;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public LegacyNotePictureMove(
      EntityPersister entityPersister,
      NotebookAttachmentContent attachmentContent,
      AcceptedWebChangeService acceptedWebChangeService,
      NoteImageFileAttachment noteImageFileAttachment,
      TestabilitySettings testabilitySettings,
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.entityPersister = entityPersister;
    this.attachmentContent = attachmentContent;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.noteImageFileAttachment = noteImageFileAttachment;
    this.testabilitySettings = testabilitySettings;
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
  }

  /**
   * @return how many references to an upload not owned in this notebook were left unchanged
   */
  @Transactional(rollbackFor = Exception.class)
  public int move(Integer notebookId) throws IOException, UnexpectedNoAccessRightException {
    Map<Note, Image> referenced = referencedLegacyPictures(notebookId);
    Map<Note, Image> pictures = new LinkedHashMap<>();
    referenced.forEach(
        (note, image) -> {
          if (ownedIn(notebookId, image)) {
            pictures.put(note, image);
          }
        });
    int skippedReferences = referenced.size() - pictures.size();
    if (pictures.isEmpty()) {
      return skippedReferences;
    }
    Map<Note, String> filenames = freeFilenames(notebookId, pictures);
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
                      note, filenames.get(note), pointers.get(image), note.getUpdatedAt());
                }
              });
          return null;
        },
        ignored -> "Move uploaded pictures into the notebook",
        testabilitySettings.getCurrentUTCTimestamp());
    return skippedReferences;
  }

  private static boolean ownedIn(Integer notebookId, Image image) {
    return image.getNote() != null && image.getNote().getNotebook().getId().equals(notebookId);
  }

  private Map<Note, Image> referencedLegacyPictures(Integer notebookId) {
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
          .ifPresent(image -> pictures.put(note, image));
    }
    return pictures;
  }

  /**
   * A name is taken when the accepted tree has a file, note or folder at that path, or this move
   * already chose it.
   */
  private Map<Note, String> freeFilenames(Integer notebookId, Map<Note, Image> pictures) {
    Set<String> chosenPaths = new HashSet<>();
    Map<Note, String> filenames = new HashMap<>();
    try (OpenedAcceptedRepository accepted =
        repositoryStore.open(bindingRepository.findByNotebook_Id(notebookId).orElseThrow())) {
      Predicate<String> acceptedTaken =
          NotebookGitAcceptedTree.takenPaths(accepted.repository(), accepted.head());
      pictures.forEach(
          (note, image) -> {
            String folderPath = NotebookGitPortablePath.folderPath(note.getFolder());
            String filename =
                NumberedNameSelection.firstAvailableFilename(
                    plainFilename(image.getName()),
                    candidate -> {
                      String path = NotebookGitPortablePath.ofAttachment(folderPath, candidate);
                      return chosenPaths.contains(path) || acceptedTaken.test(path);
                    });
            chosenPaths.add(NotebookGitPortablePath.ofAttachment(folderPath, filename));
            filenames.put(note, filename);
          });
    }
    return filenames;
  }

  /** The stored name's last segment; a hidden or empty one becomes {@code picture.png}-like. */
  private static String plainFilename(String storedName) {
    String name = storedName.substring(storedName.lastIndexOf('/') + 1);
    return NotebookGitPortablePath.isPlainFilename(name) ? name : "picture" + name;
  }

  private static Optional<Integer> legacyImageId(Note note) {
    return NoteContentMarkdown.leadingFrontmatterImageReference(note.getContent())
            instanceof Referenced referenced
        ? Optional.of(referenced.imageId())
        : Optional.empty();
  }
}

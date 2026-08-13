package com.odde.doughnut.testability;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.DisplayName;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.CircleRepository;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.model.NotesTestData;
import com.odde.doughnut.testability.model.NotesTestData.NoteTestData;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"e2e", "test"})
class InjectNotesWorker {
  @Autowired NotebookRepository notebookRepository;
  @Autowired CircleRepository circleRepository;
  @Autowired EntityPersister entityPersister;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookService notebookService;
  @Autowired FolderRepository folderRepository;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  Map<String, Integer> inject(NotesTestData notesTestData, User user) {
    if (Strings.isEmpty(notesTestData.getNotebookName())) {
      throw new RuntimeException("notebookName is required and cannot be empty");
    }
    Ownership ownership = getOwnership(notesTestData, user);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    List<NoteTestData> injections =
        Optional.ofNullable(notesTestData.getNoteTestData()).orElseGet(Collections::emptyList);
    notesTestData.setNoteTestData(injections);
    Notebook notebook = findOwnedOrCreate(notesTestData, ownership, user, currentUTCTimestamp);
    if (injections.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Note> titleNoteMap = notesTestData.buildIndividualNotes(currentUTCTimestamp);
    notesTestData.buildNoteTree(notebook, currentUTCTimestamp, titleNoteMap, this.entityPersister);
    applyExplicitFolderPlacements(injections, titleNoteMap, currentUTCTimestamp);
    notesTestData.saveByOriginalOrder(titleNoteMap, this.entityPersister);
    for (Note note : titleNoteMap.values()) {
      wikiTitleCacheService.refreshForNote(note, user);
    }
    return titleNoteMap.values().stream()
        .collect(Collectors.toMap(note -> note.getTitle(), Note::getId));
  }

  private void applyExplicitFolderPlacements(
      List<NoteTestData> injections, Map<String, Note> titleNoteMap, Timestamp now) {
    for (NoteTestData injection : injections) {
      if (Strings.isBlank(injection.getFolder())) {
        continue;
      }
      Note note = titleNoteMap.get(injection.title);
      Folder folder = resolveOrCreateFolderPath(note.getNotebook(), injection.getFolder(), now);
      note.setFolder(folder);
    }
  }

  private Folder resolveOrCreateFolderPath(Notebook notebook, String folderPath, Timestamp now) {
    Folder parent = null;
    for (String rawSegment : folderPath.split("/")) {
      String name = rawSegment.trim();
      if (name.isEmpty()) {
        continue;
      }
      Integer parentFolderId = parent == null ? null : parent.getId();
      DisplayName segmentName = new DisplayName(name);
      List<Folder> candidates =
          folderRepository.findCandidateChildContainers(
              notebook.getId(), parentFolderId, segmentName);
      if (!candidates.isEmpty()) {
        parent = candidates.getFirst();
        continue;
      }
      Folder created = new Folder();
      created.setNotebook(notebook);
      created.setParentFolder(parent);
      created.setName(segmentName);
      created.setCreatedAt(now);
      created.setUpdatedAt(now);
      entityPersister.save(created);
      parent = created;
    }
    if (parent == null) {
      throw new RuntimeException("Folder path resolved to no folder: `" + folderPath + "`");
    }
    return parent;
  }

  private Notebook findOwnedOrCreate(
      NotesTestData notesTestData, Ownership ownership, User user, Timestamp currentUTCTimestamp) {
    String notebookName = notesTestData.getNotebookName();
    return notebookRepository
        .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(new DisplayName(notebookName))
        .map(nb -> requireMatchingOwnership(nb, ownership, notebookName))
        .orElseGet(
            () ->
                notebookService.createNotebookForOwnership(
                    ownership, user, currentUTCTimestamp, notebookName, null));
  }

  private Notebook requireMatchingOwnership(
      Notebook notebook, Ownership ownership, String notebookName) {
    if (!Objects.equals(notebook.getOwnership().getId(), ownership.getId())) {
      throw new RuntimeException(
          "Notebook named `" + notebookName + "` exists but belongs to different ownership.");
    }
    return notebook;
  }

  private Ownership getOwnership(NotesTestData notesTestData, User user) {
    if (notesTestData.getCircleName() != null) {
      Circle circle = circleRepository.findByName(notesTestData.getCircleName());
      return circle.getOwnership();
    }
    return user.getOwnership();
  }
}

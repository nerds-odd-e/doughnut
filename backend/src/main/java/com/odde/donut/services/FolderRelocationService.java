package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteTitlePlacementRules noteTitlePlacementRules;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final FolderSubtree subtree;
  private final FolderMoveRelocation folderMoveRelocation;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteTitlePlacementRules noteTitlePlacementRules,
      WikiLinkRewriteService wikiLinkRewriteService,
      ResolvedWikiLinkService resolvedWikiLinkService) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.subtree =
        new FolderSubtree(
            folderRepository, noteRepository, entityPersister, noteTitlePlacementRules);
    this.folderMoveRelocation =
        new FolderMoveRelocation(
            folderRepository,
            folderSiblingNameValidation,
            entityPersister,
            testabilitySettings,
            wikiLinkRewriteService,
            resolvedWikiLinkService,
            subtree);
  }

  public Folder moveFolder(
      Notebook notebook,
      Folder folder,
      FolderMoveRequest request,
      Notebook destinationNotebook,
      User viewer) {
    return folderMoveRelocation.moveFolder(notebook, folder, request, destinationNotebook, viewer);
  }

  public Folder renameFolder(
      Notebook notebook, Folder folder, FolderRenameRequest request, User viewer) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    DisplayName displayName = new DisplayName(request.getName());
    String oldName = folder.getName();
    if (displayName.value().equals(oldName)) {
      return folder;
    }
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    folderSiblingNameValidation.requireNoConflictingSibling(
        notebook.getId(), parentFolderId, displayName, folder.getId());
    Set<Integer> noteIdsInSubtree = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(
            noteIdsInSubtree, viewer);
    folder.setName(displayName);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    wikiLinkRewriteService.rewriteInboundWikiLinksForFolderRename(
        noteIdsInSubtree, oldName, displayName.value(), now, viewer, inboundReferencesByNoteId);
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder, boolean merge, User viewer) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Set<Integer> affectedNoteIds = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(
            affectedNoteIds, viewer);

    List<Folder> directSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());

    for (Folder child : directSubfolders) {
      Optional<Folder> existingSibling =
          folderSiblingNameValidation.findConflictingSibling(
              notebook.getId(), destinationId, new DisplayName(child.getName()), folder.getId());
      if (existingSibling.isEmpty()) {
        continue;
      }
      if (merge) {
        subtree.mergeInto(child, existingSibling.get(), now);
      } else {
        FolderSiblingNameValidation.throwFolderNameConflict(
            FolderSiblingNameValidation.dissolveSiblingClashAtDestination(child.getName()));
      }
    }

    List<Folder> remainingSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());
    for (Folder child : remainingSubfolders) {
      child.setParentFolder(destination);
      child.setUpdatedAt(now);
      entityPersister.merge(child);
    }

    List<Note> directNotes = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    for (Note note : directNotes) {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, destination, note.getTitle());
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
    entityPersister.flush();
    wikiLinkRewriteService.rewriteInboundWikiLinksForFolderReparent(
        affectedNoteIds, now, viewer, inboundReferencesByNoteId);
  }
}

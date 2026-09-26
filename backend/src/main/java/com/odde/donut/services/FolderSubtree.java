package com.odde.donut.services;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Walks a folder tree and applies subtree reassignment, merge, dissolve and removal. */
@Service
final class FolderSubtree {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final NotebookAttachmentRepository notebookAttachmentRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final FolderContentsPlacementCheck contentsPlacementCheck;
  private final EntityPersister entityPersister;

  FolderSubtree(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      NotebookAttachmentRepository notebookAttachmentRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      FolderContentsPlacementCheck contentsPlacementCheck,
      EntityPersister entityPersister) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.contentsPlacementCheck = contentsPlacementCheck;
    this.entityPersister = entityPersister;
  }

  List<Note> collectNotes(List<Folder> subtreeFolders) {
    List<Note> notes = new ArrayList<>();
    for (Folder subtreeFolder : subtreeFolders) {
      notes.addAll(noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId()));
    }
    return notes;
  }

  List<NotebookAttachment> collectAttachments(List<Folder> subtreeFolders) {
    return notebookAttachmentRepository.findByFolder_IdIn(
        subtreeFolders.stream().map(Folder::getId).toList());
  }

  Set<Integer> collectNoteIds(List<Folder> subtreeFolders) {
    Set<Integer> noteIds = new LinkedHashSet<>();
    for (Note note : collectNotes(subtreeFolders)) {
      noteIds.add(note.getId());
    }
    return noteIds;
  }

  Set<Integer> collectNoteIdsInSubtree(Folder root) {
    return collectNoteIds(collectFolders(root));
  }

  List<Folder> collectFolders(Folder root) {
    List<Folder> result = new ArrayList<>();
    Deque<Folder> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
      Folder current = stack.pop();
      result.add(current);
      for (Folder child :
          folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(current.getId())) {
        stack.push(child);
      }
    }
    return result;
  }

  void reassignToNotebook(
      List<Folder> subtreeFolders, Notebook destinationNotebook, Timestamp now) {
    requireSubtreeHasNoAttachments(subtreeFolders.getFirst());
    for (Folder subtreeFolder : subtreeFolders) {
      subtreeFolder.setNotebook(destinationNotebook);
      subtreeFolder.setUpdatedAt(now);
      entityPersister.merge(subtreeFolder);
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        note.assignNotebook(destinationNotebook);
        entityPersister.merge(note);
      }
    }
  }

  /**
   * Moves {@code folder}'s contents to its parent and removes the folder itself, after checking
   * every entry it would create there. A subfolder whose name a folder at the destination holds
   * (ignoring case) is merged into it when {@code merge} is set, and refused otherwise.
   */
  void dissolveInto(Folder folder, boolean merge, Timestamp now) {
    requireSubtreeHasNoAttachments(folder);
    Folder destination = folder.getParentFolder();
    Set<Integer> excluded = Set.of(folder.getId());
    contentsPlacementCheck.requireContentsFit(folder, destination, merge, excluded);
    moveContentsInto(folder, destination, folder.getNotebook(), excluded, now);
    entityPersister.flush();
    entityPersister.remove(folder);
    entityPersister.flush();
  }

  /** Merges {@code source} into {@code target} in the same notebook, after checking every entry. */
  void mergeWithinNotebook(Folder source, Folder target, Timestamp now) {
    contentsPlacementCheck.requireContentsFit(source, target, true, Set.of());
    mergeInto(source, target, now);
  }

  void mergeInto(Folder source, Folder target, Timestamp now) {
    requireSubtreeHasNoAttachments(source);
    moveContentsInto(source, target, target.getNotebook(), Set.of(), now);
    target.setUpdatedAt(now);
    entityPersister.merge(target);
    entityPersister.flush();
    entityPersister.remove(source);
  }

  /**
   * Places {@code source}'s subfolders and notes in {@code destinationOrNull}; a subfolder whose
   * name a folder there holds (ignoring case) is merged into it.
   */
  private void moveContentsInto(
      Folder source,
      Folder destinationOrNull,
      Notebook destinationNotebook,
      Set<Integer> excludedFolderIds,
      Timestamp now) {
    boolean crossNotebook = !source.getNotebook().getId().equals(destinationNotebook.getId());
    for (Folder child :
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(source.getId())) {
      Optional<Folder> existing =
          folderSiblingNameValidation.folderHolding(
              destinationNotebook, destinationOrNull, child.getName(), excludedFolderIds);
      if (existing.isPresent()) {
        mergeInto(child, existing.get(), now);
      } else {
        child.setParentFolder(destinationOrNull);
        child.setUpdatedAt(now);
        if (crossNotebook) {
          reassignToNotebook(collectFolders(child), destinationNotebook, now);
        }
        entityPersister.merge(child);
      }
    }
    for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(source.getId())) {
      note.setFolder(destinationOrNull);
      if (crossNotebook) {
        note.assignNotebook(destinationNotebook);
      }
      entityPersister.merge(note);
    }
  }

  private void requireSubtreeHasNoAttachments(Folder source) {
    List<Integer> folderIds = collectFolders(source).stream().map(Folder::getId).toList();
    if (notebookAttachmentRepository.existsByFolder_IdIn(folderIds)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Folders containing files cannot be dissolved, merged, or moved to another notebook yet.");
    }
  }
}

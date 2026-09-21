package com.odde.donut.services;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
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
  private final EntityPersister entityPersister;

  FolderSubtree(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      NotebookAttachmentRepository notebookAttachmentRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
  }

  List<Note> collectNotes(List<Folder> subtreeFolders) {
    List<Note> notes = new ArrayList<>();
    for (Folder subtreeFolder : subtreeFolders) {
      notes.addAll(noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId()));
    }
    return notes;
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
   * Reassigns {@code folder}'s direct subfolders and notes to its parent and removes the folder
   * itself. A subfolder whose name already exists at the destination is merged into that sibling
   * when {@code merge} is set, and refused otherwise.
   */
  void dissolveInto(Folder folder, boolean merge, Timestamp now) {
    requireSubtreeHasNoAttachments(folder);
    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();

    List<Folder> directSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());

    for (Folder child : directSubfolders) {
      Optional<Folder> existingSibling =
          folderSiblingNameValidation.findConflictingSibling(
              folder.getNotebook().getId(),
              destinationId,
              new DisplayName(child.getName()),
              folder.getId());
      if (existingSibling.isEmpty()) {
        continue;
      }
      if (merge) {
        mergeInto(child, existingSibling.get(), now);
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
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
    entityPersister.flush();
  }

  void mergeInto(Folder source, Folder target, Timestamp now) {
    requireSubtreeHasNoAttachments(source);
    Notebook destinationNotebook = target.getNotebook();
    boolean crossNotebook = !source.getNotebook().getId().equals(destinationNotebook.getId());

    List<Folder> srcSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(source.getId());
    for (Folder srcChild : srcSubfolders) {
      Optional<Folder> tgtChild =
          folderRepository
              .findCandidateChildContainers(
                  destinationNotebook.getId(), target.getId(), new DisplayName(srcChild.getName()))
              .stream()
              .findFirst();
      if (tgtChild.isPresent()) {
        mergeInto(srcChild, tgtChild.get(), now);
      } else {
        srcChild.setParentFolder(target);
        srcChild.setUpdatedAt(now);
        if (crossNotebook) {
          reassignToNotebook(collectFolders(srcChild), destinationNotebook, now);
        }
        entityPersister.merge(srcChild);
      }
    }

    List<Note> srcNotes = noteRepository.findNotesInFolderOrderByIdAsc(source.getId());
    for (Note note : srcNotes) {
      note.setFolder(target);
      if (crossNotebook) {
        note.assignNotebook(destinationNotebook);
      }
      entityPersister.merge(note);
    }

    target.setUpdatedAt(now);
    entityPersister.merge(target);
    entityPersister.flush();
    entityPersister.remove(source);
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

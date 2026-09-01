package com.odde.donut.services;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Walks a folder tree and applies subtree reassignment and merge. */
final class FolderSubtree {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final NoteTitlePlacementRules noteTitlePlacementRules;

  FolderSubtree(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      NoteTitlePlacementRules noteTitlePlacementRules) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
  }

  Set<Integer> collectNoteIds(List<Folder> subtreeFolders) {
    Set<Integer> noteIds = new LinkedHashSet<>();
    for (Folder subtreeFolder : subtreeFolders) {
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        noteIds.add(note.getId());
      }
    }
    return noteIds;
  }

  Set<Integer> collectNoteIdsInSubtree(Folder root) {
    return collectNoteIds(collectFolders(root));
  }

  void requireNoSoftDeletedTitles(Notebook destinationNotebook, List<Folder> subtreeFolders) {
    for (Folder subtreeFolder : subtreeFolders) {
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
            destinationNotebook, subtreeFolder, note.getTitle());
      }
    }
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

  void mergeInto(Folder source, Folder target, Timestamp now) {
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
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
          destinationNotebook, target, note.getTitle());
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
}

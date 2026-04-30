package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityPersister entityPersister;
  private final NoteChildContainerFolderService noteChildContainerFolderService;
  private final WikiSlugPathService wikiSlugPathService;

  public NoteMotionService(
      EntityPersister entityPersister,
      NoteChildContainerFolderService noteChildContainerFolderService,
      WikiSlugPathService wikiSlugPathService) {
    this.entityPersister = entityPersister;
    this.noteChildContainerFolderService = noteChildContainerFolderService;
    this.wikiSlugPathService = wikiSlugPathService;
  }

  public void execute(Note subject, Note relativeToNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException {
    validateNoCyclicLink(subject, relativeToNote);
    if (asFirstChildOfNote) {
      subject.updateSiblingOrderAsFirstChild(relativeToNote);
      subject.setParentNote(relativeToNote);
    } else {
      subject.setSiblingOrderToInsertAfter(relativeToNote);
      subject.setParentNote(relativeToNote.getParent());
    }
    subject.adjustPositionAsAChildOfParentInMemory();
    assignUniquePlaceholderSlugsPendingFolderAlign(subject);
    entityPersister.flush();
    alignFoldersForNoteAndDescendants(subject);
    entityPersister.flush();
    recomputeSlugPathsForNoteSubtree(subject);

    // Save all descendants as their notebooks have changed
    subject.getAllDescendants().forEach(entityPersister::merge);
    entityPersister.merge(subject);
    entityPersister.flush();
  }

  private void validateNoCyclicLink(Note subject, Note relativeToNote)
      throws CyclicLinkDetectedException {
    if (relativeToNote.getAncestors().contains(subject)) {
      throw new CyclicLinkDetectedException();
    }
  }

  public void validate(Note subject, Note relativeToNote, boolean asFirstChildOfNote)
      throws MovementNotPossibleException {
    if (asFirstChildOfNote
        && relativeToNote.getChildren().stream()
            .findFirst()
            .map(n -> n.getId().equals(subject.getId()))
            .orElse(false)) {
      throw new MovementNotPossibleException();
    }
    if (!asFirstChildOfNote && relativeToNote.getParent() == null) {
      throw new MovementNotPossibleException();
    }
  }

  public void executeMoveUnder(Note sourceNote, Note targetNote, Boolean asFirstChild)
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    if (!asFirstChild) {
      List<Note> children = targetNote.getChildren();
      if (!children.isEmpty()) {
        Note lastChild = children.getLast();
        validate(sourceNote, lastChild, false);
        execute(sourceNote, lastChild, false);
        return;
      }
    }
    validate(sourceNote, targetNote, true);
    execute(sourceNote, targetNote, true);
  }

  public void moveToTopLevel(Note note, User user) {
    note.detachFromParentInMemory();
    note.setFolder(null);
    assignUniquePlaceholderSlugsPendingFolderAlign(note);
    entityPersister.flush();
    alignFoldersForNoteAndDescendants(note);
    entityPersister.flush();
    recomputeSlugPathsForNoteSubtree(note);
    note.getAllDescendants().forEach(entityPersister::merge);
    entityPersister.merge(note);
    entityPersister.flush();
  }

  private void recomputeSlugPathsForNoteSubtree(Note root) {
    Stream.concat(Stream.of(root), root.getAllDescendants())
        .forEach(wikiSlugPathService::assignSlugForNewNote);
  }

  private void assignUniquePlaceholderSlugsPendingFolderAlign(Note root) {
    Stream.concat(Stream.of(root), root.getAllDescendants())
        .filter(n -> n.getId() != null)
        .forEach(n -> n.setSlug("z-wip-mv-" + n.getId()));
  }

  private void alignFoldersForNoteAndDescendants(Note root) {
    alignFolderForSingleNote(root);
    root.getAllDescendants().forEach(this::alignFolderForSingleNote);
  }

  private void alignFolderForSingleNote(Note note) {
    Note parent = note.getParent();
    if (parent == null) {
      note.setFolder(null);
    } else {
      note.setFolder(noteChildContainerFolderService.resolveForParent(parent));
    }
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Invariants for moving a folder to a new parent in the same notebook. */
public final class FolderMoveDestinationRules {

  private FolderMoveDestinationRules() {}

  public static void requireNotMovingIntoSelfOrDescendant(Folder folder, Folder newParent) {
    if (newParent == null) {
      return;
    }
    if (newParent.getId().equals(folder.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move folder into itself.");
    }
    if (folderIsStrictDescendantOf(folder, newParent)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot move folder into its descendant.");
    }
  }

  /** True if {@code possibleDescendant} is {@code ancestor} or strictly under {@code ancestor}. */
  private static boolean folderIsStrictDescendantOf(Folder ancestor, Folder possibleDescendant) {
    Folder x = possibleDescendant;
    while (x != null) {
      if (x.getId().equals(ancestor.getId())) {
        return true;
      }
      x = x.getParentFolder();
    }
    return false;
  }
}

package com.odde.donut.services;

import com.odde.donut.entities.Folder;
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
    if (newParent.trailFromRoot().stream().anyMatch(f -> f.getId().equals(folder.getId()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot move folder into its descendant.");
    }
  }
}

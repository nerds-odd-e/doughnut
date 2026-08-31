package com.odde.donut.services;

import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortablePathAuthoring {

  public String authoredPortablePath(Note destinationNote, String originalPortablePath) {
    String notePortion = normalizedNotePortion(destinationNote);
    if (originalPortablePath == null || originalPortablePath.isBlank()) {
      return notePortion;
    }
    PortablePath original = PortablePath.parse(originalPortablePath);
    return new PortablePath(Optional.empty(), notePortion, original.encodedPropertyKey()).format();
  }

  private static String normalizedNotePortion(Note note) {
    List<String> folders = FolderTrailSegments.namesFromRootToContainingFolder(note);
    String title = note.getTitle();
    if (folders.isEmpty()) {
      return title;
    }
    return String.join("/", folders) + "/" + title;
  }
}

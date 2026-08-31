package com.odde.donut.services;

import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortablePathAuthoring {

  private final WikiLinkResolver wikiLinkResolver;

  public PortablePathAuthoring(WikiLinkResolver wikiLinkResolver) {
    this.wikiLinkResolver = wikiLinkResolver;
  }

  public String authoredPortablePath(
      Note sourceNote, Note destinationNote, String originalPortablePath) {
    String notePortion = shortestUnambiguousNotePortion(destinationNote);
    Optional<String> encodedPropertyKey =
        (originalPortablePath == null || originalPortablePath.isBlank())
            ? Optional.empty()
            : PortablePath.parse(originalPortablePath).encodedPropertyKey();
    PortablePath authored = new PortablePath(Optional.empty(), notePortion, encodedPropertyKey);
    if (!sourceNote.getNotebook().getId().equals(destinationNote.getNotebook().getId())) {
      authored = authored.withNotebookName(destinationNote.getNotebook().getName());
    }
    return authored.format();
  }

  private String shortestUnambiguousNotePortion(Note destinationNote) {
    if (displayNameUniquelyIdentifies(destinationNote)) {
      return destinationNote.getTitle();
    }
    return lengthenedNotePortion(destinationNote);
  }

  private boolean displayNameUniquelyIdentifies(Note destinationNote) {
    return wikiLinkResolver
        .resolveAnyTargetWikiLinkToken(destinationNote.getTitle(), destinationNote)
        .filter(match -> match.getId().equals(destinationNote.getId()))
        .isPresent();
  }

  private static String lengthenedNotePortion(Note note) {
    List<String> folders = FolderTrailSegments.namesFromRootToContainingFolder(note);
    String title = note.getTitle();
    if (folders.isEmpty()) {
      return "/" + title;
    }
    return String.join("/", folders) + "/" + title;
  }
}

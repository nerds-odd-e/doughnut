package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.controllers.dto.WikiLink;

/** Builds {@link WikiLink} DTOs from authored note references. */
final class WikiLinks {

  private WikiLinks() {}

  static WikiLink resolvedFromStoredAuthoredLink(String authoredLink, Integer destinationId) {
    return fromReference(
        AuthoredNoteReferences.fromStoredAuthoredLink(authoredLink),
        WikiLink.Resolution.RESOLVED,
        destinationId);
  }

  static WikiLink ambiguous(AuthoredNoteReference.WikiPortablePathTarget wiki) {
    return fromReference(wiki, WikiLink.Resolution.AMBIGUOUS, null);
  }

  private static WikiLink fromReference(
      AuthoredNoteReference ref, WikiLink.Resolution resolution, Integer destinationId) {
    return switch (ref) {
      case AuthoredNoteReference.WikiPortablePathTarget wiki ->
          new WikiLink(
              wiki.authoredLink(),
              wiki.portablePath().format(),
              wiki.displayText(),
              resolution,
              destinationId);
      case AuthoredNoteReference.NoteIdUrlTarget url ->
          new WikiLink(
              url.authoredLink(), url.href(), url.displayText(), resolution, destinationId);
    };
  }
}

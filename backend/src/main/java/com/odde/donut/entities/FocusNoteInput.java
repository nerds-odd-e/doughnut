package com.odde.donut.entities;

import java.sql.Timestamp;

/** The focus note a recall question was generated from, as read when preparing the request. */
public record FocusNoteInput(Integer noteId, Timestamp readAt, String fingerprint) {
  public boolean wasReadAfterLastUpdateOf(Note focus) {
    return focus.getId().equals(noteId) && readAt != null && focus.getUpdatedAt().before(readAt);
  }
}

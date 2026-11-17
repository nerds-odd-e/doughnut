package com.odde.donut.testability;

import com.odde.donut.controllers.dto.NoteRealm;
import java.util.List;

public final class NoteRealmReferences {
  private NoteRealmReferences() {}

  public static List<Integer> referrerIds(NoteRealm realm) {
    return realm.getReferences().stream().map(entry -> entry.noteTopology().getId()).toList();
  }
}

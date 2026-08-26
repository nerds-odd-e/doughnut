package com.odde.donut.testability;

import com.odde.donut.controllers.dto.NoteSearchResult;
import com.odde.donut.controllers.dto.RelationshipLiteralSearchHit;
import java.util.List;

public final class RelationshipLiteralSearchHits {
  private RelationshipLiteralSearchHits() {}

  public static List<NoteSearchResult> noteMatches(List<RelationshipLiteralSearchHit> hits) {
    return hits.stream()
        .filter(RelationshipLiteralSearchHit::isNote)
        .map(RelationshipLiteralSearchHit::getNoteSearchResult)
        .toList();
  }
}

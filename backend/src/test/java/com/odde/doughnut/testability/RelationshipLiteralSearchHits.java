package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
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

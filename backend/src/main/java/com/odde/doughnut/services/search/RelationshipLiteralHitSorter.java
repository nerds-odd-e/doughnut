package com.odde.doughnut.services.search;

import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RelationshipLiteralHitSorter {
  public List<RelationshipLiteralSearchHit> sort(
      List<RelationshipLiteralSearchHit> hits, Integer notebookId) {
    Comparator<RelationshipLiteralSearchHit> byDistance =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? (h.getNoteSearchResult().getDistance() != null
                        ? h.getNoteSearchResult().getDistance()
                        : Float.MAX_VALUE)
                    : (h.getDistance() != null ? h.getDistance() : Float.MAX_VALUE));
    Comparator<RelationshipLiteralSearchHit> byNotebook = byNotebook(notebookId);
    Comparator<RelationshipLiteralSearchHit> byLabel =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? h.getNoteSearchResult().getNoteTopology().getTitle()
                    : (h.isFolder() ? h.getFolderName() : h.getNotebookName()),
            String.CASE_INSENSITIVE_ORDER);
    Comparator<RelationshipLiteralSearchHit> byId =
        Comparator.comparing(
            h ->
                h.isNote()
                    ? h.getNoteSearchResult().getNoteTopology().getId()
                    : (h.isFolder() ? h.getFolderId() : h.getNotebookId()));
    return hits.stream()
        .sorted(byDistance.thenComparing(byNotebook).thenComparing(byLabel).thenComparing(byId))
        .toList();
  }

  private Comparator<RelationshipLiteralSearchHit> byNotebook(Integer notebookId) {
    return (a, b) -> {
      if (notebookId == null) {
        return 0;
      }
      boolean aSame =
          notebookId.equals(
              a.isNote() ? a.getNoteSearchResult().getNotebookId() : a.getNotebookId());
      boolean bSame =
          notebookId.equals(
              b.isNote() ? b.getNoteSearchResult().getNotebookId() : b.getNotebookId());
      return Boolean.compare(bSame, aSame);
    };
  }
}

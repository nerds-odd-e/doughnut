package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.NoteService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FocusContextFolderPeerAppender {
  private final NoteRepository noteRepository;
  private final NoteService noteService;

  FocusContextFolderPeerAppender(NoteRepository noteRepository, NoteService noteService) {
    this.noteRepository = noteRepository;
    this.noteService = noteService;
  }

  record SiblingAnchor(Note note, int wikiDepth, List<String> pathToAnchorWikiUris) {}

  private record SiblingOffer(
      int noteId, int anchorWikiDepth, int anchorIndex, List<String> pathToAnchor) {}

  void append(
      FocusContextResult result,
      Integer focusId,
      List<SiblingAnchor> siblingAnchors,
      int siblingBudgetTokens,
      Set<Integer> wikiClaimedNoteIds,
      Optional<Long> sampleSeed) {
    if (siblingBudgetTokens <= 0) {
      return;
    }

    List<SiblingOffer> offers = new ArrayList<>();
    int anchorIndex = 0;
    for (SiblingAnchor anchor : siblingAnchors) {
      int cap = FocusContextConstants.sampleCapAtGraphDepth(anchor.wikiDepth + 1);
      List<Note> candidates =
          noteService.findStructuralPeerNotesSample(
              anchor.note, focusId, wikiClaimedNoteIds, cap, sampleSeed);
      for (Note p : candidates) {
        offers.add(
            new SiblingOffer(
                p.getId(),
                anchor.wikiDepth,
                anchorIndex,
                List.copyOf(anchor.pathToAnchorWikiUris)));
      }
      anchorIndex++;
    }

    offers.sort(
        Comparator.comparingInt(SiblingOffer::anchorWikiDepth)
            .thenComparingInt(SiblingOffer::anchorIndex));

    Set<Integer> reservedSiblingIds = new HashSet<>(wikiClaimedNoteIds);
    List<SiblingOffer> uniqueOffers = new ArrayList<>();
    for (SiblingOffer o : offers) {
      if (reservedSiblingIds.add(o.noteId)) {
        uniqueOffers.add(o);
      }
    }

    List<Integer> idsToHydrate = uniqueOffers.stream().map(SiblingOffer::noteId).toList();
    Map<Integer, Note> hydratedById =
        FocusContextRelatedNoteMaterializer.hydrateById(noteRepository, idsToHydrate);
    if (hydratedById.isEmpty()) {
      return;
    }

    int siblingRemaining = siblingBudgetTokens;
    for (SiblingOffer o : uniqueOffers) {
      if (siblingRemaining <= 0) {
        break;
      }
      Note hydratedNote = hydratedById.get(o.noteId);
      if (hydratedNote == null) {
        continue;
      }
      var materialized =
          FocusContextRelatedNoteMaterializer.materialize(
              hydratedNote, o.anchorWikiDepth + 1, o.pathToAnchor);
      if (materialized.tokenCost() > siblingRemaining) {
        continue;
      }
      siblingRemaining -= materialized.tokenCost();
      result.addRelatedNote(materialized.note());
    }
  }
}

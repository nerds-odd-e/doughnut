package com.odde.donut.services.focusContext;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.WikiTitleCacheService;
import com.odde.donut.services.focusContext.FocusContextFolderPeerAppender.SiblingAnchor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FocusContextWikiBfsExpander {
  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;

  FocusContextWikiBfsExpander(
      WikiTitleCacheService wikiTitleCacheService, NoteRepository noteRepository) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
  }

  record WikiExpansion(Set<Integer> wikiClaimedNoteIds, List<SiblingAnchor> siblingAnchors) {}

  WikiExpansion expand(
      FocusContextResult result,
      Note hydratedFocus,
      Integer focusId,
      User viewer,
      int maxDepth,
      int wikiBudgetTotal,
      Optional<Long> sampleSeed) {
    String focusWikiUri = FocusContextWikiUri.ofFocusNote(hydratedFocus);

    Map<Integer, List<String>> pathEndingAtWikiUriByNoteId = new HashMap<>();
    if (focusId != null) {
      pathEndingAtWikiUriByNoteId.put(focusId, List.of(focusWikiUri));
    }

    List<Note> frontier = new ArrayList<>();
    frontier.add(hydratedFocus);

    Set<Integer> wikiClaimedNoteIds = new HashSet<>();
    if (focusId != null) {
      wikiClaimedNoteIds.add(focusId);
    }

    List<SiblingAnchor> siblingAnchors = new ArrayList<>();
    siblingAnchors.add(new SiblingAnchor(hydratedFocus, 0, List.of(focusWikiUri)));

    int wikiRemainingBudget = wikiBudgetTotal;
    for (int depth = 1; depth <= maxDepth; depth++) {
      if (wikiRemainingBudget <= 0 || frontier.isEmpty()) {
        break;
      }

      List<Proposal> proposals =
          collectProposals(
              frontier, pathEndingAtWikiUriByNoteId, focusId, viewer, depth, sampleSeed);

      Map<Integer, Proposal> firstById = new LinkedHashMap<>();
      for (Proposal p : proposals) {
        firstById.putIfAbsent(p.noteId, p);
      }
      List<Proposal> orderedUnique = new ArrayList<>(firstById.values());

      Map<Integer, Note> hydratedById =
          FocusContextRelatedNoteMaterializer.hydrateById(
              noteRepository, orderedUnique.stream().map(p -> p.noteId).toList());

      List<Note> nextFrontier = new ArrayList<>();
      for (Proposal p : orderedUnique) {
        if (wikiRemainingBudget <= 0) {
          break;
        }
        Note hydratedNote = hydratedById.get(p.noteId);
        if (hydratedNote == null) {
          continue;
        }
        var materialized =
            FocusContextRelatedNoteMaterializer.materialize(hydratedNote, p.depth, p.retrievalPath);
        wikiRemainingBudget -= materialized.tokenCost();
        result.addRelatedNote(materialized.note());
        wikiClaimedNoteIds.add(hydratedNote.getId());
        pathEndingAtWikiUriByNoteId.put(hydratedNote.getId(), p.retrievalPath);
        nextFrontier.add(hydratedNote);
        siblingAnchors.add(new SiblingAnchor(hydratedNote, p.depth, p.retrievalPath));
      }

      frontier = nextFrontier;
    }

    return new WikiExpansion(wikiClaimedNoteIds, siblingAnchors);
  }

  private List<Proposal> collectProposals(
      List<Note> frontier,
      Map<Integer, List<String>> pathEndingAtWikiUriByNoteId,
      Integer focusId,
      User viewer,
      int depth,
      Optional<Long> sampleSeed) {
    List<Proposal> proposals = new ArrayList<>();
    for (Note parent : frontier) {
      List<String> parentPath = pathEndingAtWikiUriByNoteId.get(parent.getId());
      if (parentPath == null) {
        continue;
      }

      List<Note> outgoing =
          wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(parent, viewer);

      Set<Integer> inboundExclude = new HashSet<>();
      if (focusId != null) {
        inboundExclude.add(focusId);
      }
      for (Note o : outgoing) {
        if (o.getId() != null) {
          inboundExclude.add(o.getId());
        }
      }
      List<Note> sampledInbound =
          wikiTitleCacheService.sampledReferencesNotesForFocusContext(
              parent,
              viewer,
              inboundExclude,
              FocusContextConstants.sampleCapAtGraphDepth(depth),
              sampleSeed);

      for (Note target : outgoing) {
        if (target.getId() == null || target.getId().equals(focusId)) {
          continue;
        }
        proposals.add(new Proposal(target.getId(), depth, appendWikiUri(parentPath, target)));
      }

      for (Note target : sampledInbound) {
        proposals.add(new Proposal(target.getId(), depth, appendWikiUri(parentPath, target)));
      }
    }
    return proposals;
  }

  private static List<String> appendWikiUri(List<String> prefix, Note target) {
    List<String> path = new ArrayList<>(prefix);
    path.add(FocusContextWikiUri.of(target));
    return List.copyOf(path);
  }

  private record Proposal(int noteId, int depth, List<String> retrievalPath) {}
}

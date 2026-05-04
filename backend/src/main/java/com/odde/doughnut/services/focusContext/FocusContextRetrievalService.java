package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.WikiTitleCacheService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FocusContextRetrievalService {

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;

  @Autowired
  public FocusContextRetrievalService(
      WikiTitleCacheService wikiTitleCacheService,
      NoteRepository noteRepository,
      AuthorizationService authorizationService) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
  }

  public FocusContextResult retrieve(Note focusNote, RetrievalConfig config) {
    return retrieve(focusNote, authorizationService.getCurrentUser(), config);
  }

  public FocusContextResult retrieve(Note focusNote, User viewer, RetrievalConfig config) {
    Note hydrated =
        Optional.ofNullable(focusNote.getId())
            .flatMap(
                id ->
                    noteRepository
                        .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(id))
                        .stream()
                        .findFirst())
            .orElse(focusNote);

    String focusDetails =
        truncateToTokens(
            hydrated.getDetails(), FocusContextConstants.FOCUS_NOTE_DETAILS_MAX_TOKENS);
    boolean focusTruncated =
        focusDetails != null
            && hydrated.getDetails() != null
            && focusDetails.length() < hydrated.getDetails().length();

    FocusContextFocusNote focusNoteModel =
        new FocusContextFocusNote(
            hydrated.getNotebook() != null ? hydrated.getNotebook().getName() : null,
            hydrated.getTitle(),
            FolderTrailSegments.crumbPathJoinedBySlashSpace(hydrated),
            focusDetails,
            focusTruncated);

    FocusContextResult result = new FocusContextResult(focusNoteModel);

    if (config.getMaxDepth() < 1) {
      return result;
    }

    int remainingBudget = FocusContextConstants.RELATED_NOTES_TOTAL_BUDGET_TOKENS;
    String focusWikiUri = FocusContextWikiUri.ofFocusNote(hydrated);
    Integer focusId = hydrated.getId();

    Map<Integer, List<String>> pathEndingAtWikiUriByNoteId = new HashMap<>();
    if (focusId != null) {
      pathEndingAtWikiUriByNoteId.put(focusId, List.of(focusWikiUri));
    }

    List<Note> frontier = new ArrayList<>();
    frontier.add(hydrated);

    for (int depth = 1; depth <= config.getMaxDepth(); depth++) {
      if (remainingBudget <= 0 || frontier.isEmpty()) {
        break;
      }

      List<Proposal> proposals = new ArrayList<>();
      for (Note parent : frontier) {
        List<String> parentPath = pathEndingAtWikiUriByNoteId.get(parent.getId());
        if (parentPath == null) {
          continue;
        }

        List<Note> outgoing =
            wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(parent, viewer);
        List<Note> inbound = wikiTitleCacheService.referencesNotesForViewer(parent, viewer);

        for (Note target : outgoing) {
          if (target.getId() == null || target.getId().equals(focusId)) {
            continue;
          }
          List<String> childPath = appendWikiUri(parentPath, target);
          proposals.add(
              new Proposal(
                  target.getId(), depth, childPath, FocusContextEdgeType.OutgoingWikiLink));
        }

        for (Note target : inbound) {
          if (target.getId() == null || target.getId().equals(focusId)) {
            continue;
          }
          boolean inOutgoing =
              outgoing.stream()
                  .anyMatch(o -> o.getId() != null && o.getId().equals(target.getId()));
          if (inOutgoing) {
            continue;
          }
          List<String> childPath = appendWikiUri(parentPath, target);
          proposals.add(
              new Proposal(
                  target.getId(), depth, childPath, FocusContextEdgeType.InboundWikiReference));
        }
      }

      Map<Integer, Proposal> bestById = new HashMap<>();
      for (Proposal p : proposals) {
        Proposal existing = bestById.get(p.noteId);
        if (existing == null || beats(p, existing)) {
          bestById.put(p.noteId, p);
        }
      }

      List<Proposal> orderedUnique = new ArrayList<>();
      Set<Integer> seenWinnerIds = new HashSet<>();
      for (Proposal p : proposals) {
        Proposal winner = bestById.get(p.noteId);
        if (winner != null && winner.equals(p) && seenWinnerIds.add(p.noteId)) {
          orderedUnique.add(p);
        }
      }

      List<Integer> idsToHydrate = orderedUnique.stream().map(p -> p.noteId).toList();
      Map<Integer, Note> hydratedById = new LinkedHashMap<>();
      if (!idsToHydrate.isEmpty()) {
        for (Note n :
            noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(idsToHydrate)) {
          hydratedById.put(n.getId(), n);
        }
      }

      List<Note> nextFrontier = new ArrayList<>();
      for (Proposal p : orderedUnique) {
        if (remainingBudget <= 0) {
          break;
        }
        Note hydratedNote = hydratedById.get(p.noteId);
        if (hydratedNote == null) {
          continue;
        }
        String details =
            truncateToTokens(
                hydratedNote.getDetails(), FocusContextConstants.RELATED_NOTE_DETAILS_MAX_TOKENS);
        boolean truncated =
            details != null
                && hydratedNote.getDetails() != null
                && details.length() < hydratedNote.getDetails().length();
        int cost = estimateTokens(details);
        remainingBudget -= cost;
        result.addRelatedNote(
            new FocusContextNote(
                hydratedNote.getNotebook() != null ? hydratedNote.getNotebook().getName() : null,
                hydratedNote.getTitle(),
                FolderTrailSegments.crumbPathJoinedBySlashSpace(hydratedNote),
                p.depth,
                p.retrievalPath,
                p.edgeType,
                details,
                truncated));
        pathEndingAtWikiUriByNoteId.put(hydratedNote.getId(), p.retrievalPath);
        nextFrontier.add(hydratedNote);
      }

      frontier = nextFrontier;
    }

    return result;
  }

  private static List<String> appendWikiUri(List<String> prefix, Note target) {
    List<String> path = new ArrayList<>(prefix);
    path.add(FocusContextWikiUri.of(target));
    return List.copyOf(path);
  }

  private static boolean beats(Proposal candidate, Proposal existing) {
    if (candidate.depth < existing.depth) {
      return true;
    }
    if (candidate.depth > existing.depth) {
      return false;
    }
    if (candidate.edgeType == FocusContextEdgeType.OutgoingWikiLink
        && existing.edgeType == FocusContextEdgeType.InboundWikiReference) {
      return true;
    }
    if (candidate.edgeType == FocusContextEdgeType.InboundWikiReference
        && existing.edgeType == FocusContextEdgeType.OutgoingWikiLink) {
      return false;
    }
    return false;
  }

  private record Proposal(
      int noteId, int depth, List<String> retrievalPath, FocusContextEdgeType edgeType) {}

  private static String truncateToTokens(String text, int maxTokens) {
    if (text == null || text.isEmpty()) return text;
    int maxBytes = (int) Math.floor(maxTokens * FocusContextConstants.BYTES_PER_TOKEN);
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= maxBytes) return text;

    int low = 0;
    int high = text.length();
    while (low < high) {
      int mid = (low + high + 1) / 2;
      int len = text.substring(0, mid).getBytes(StandardCharsets.UTF_8).length;
      if (len <= maxBytes) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    return text.substring(0, low);
  }

  private static int estimateTokens(String text) {
    if (text == null || text.isEmpty()) return 0;
    return (int)
        Math.ceil(
            text.getBytes(StandardCharsets.UTF_8).length / FocusContextConstants.BYTES_PER_TOKEN);
  }
}

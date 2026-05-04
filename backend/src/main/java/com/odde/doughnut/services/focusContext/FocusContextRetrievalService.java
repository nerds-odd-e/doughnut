package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.WikiTitleCacheService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FocusContextRetrievalService {

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final NoteService noteService;

  @Autowired
  public FocusContextRetrievalService(
      WikiTitleCacheService wikiTitleCacheService,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      NoteService noteService) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.noteService = noteService;
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

    Integer focusId = hydrated.getId();

    List<String> outgoingLinkUris =
        wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(hydrated, viewer).stream()
            .map(FocusContextWikiUri::of)
            .toList();
    List<String> inboundRefUris =
        wikiTitleCacheService.referencesNotesForViewer(hydrated, viewer).stream()
            .map(FocusContextWikiUri::of)
            .toList();
    List<String> sampleSiblingUris = new ArrayList<>();
    for (Note peer : noteService.findStructuralPeerNotesInOrder(hydrated)) {
      if (peer.getId() == null || peer.getId().equals(focusId)) {
        continue;
      }
      sampleSiblingUris.add(FocusContextWikiUri.of(peer));
      if (sampleSiblingUris.size() >= FocusContextConstants.MAX_FOLDER_SIBLINGS_PER_NOTE) {
        break;
      }
    }

    FocusContextFocusNote focusNoteModel =
        new FocusContextFocusNote(
            hydrated.getNotebook() != null ? hydrated.getNotebook().getName() : null,
            hydrated.getTitle(),
            FolderTrailSegments.crumbPathJoinedBySlashSpace(hydrated),
            0,
            outgoingLinkUris,
            inboundRefUris,
            List.copyOf(sampleSiblingUris),
            hydrated.getCreatedAt(),
            focusDetails,
            focusTruncated);

    FocusContextResult result = new FocusContextResult(focusNoteModel);

    if (config.getMaxDepth() < 1 || config.getRelatedNotesTotalBudgetTokens() <= 0) {
      return result;
    }

    int relatedTotal = config.getRelatedNotesTotalBudgetTokens();
    int wikiBudgetTotal =
        (int) Math.floor(relatedTotal * FocusContextConstants.RELATED_NOTES_WIKI_BUDGET_FRACTION);
    int siblingBudgetTotal = relatedTotal - wikiBudgetTotal;

    int wikiRemainingBudget = wikiBudgetTotal;
    String focusWikiUri = FocusContextWikiUri.ofFocusNote(hydrated);

    Map<Integer, List<String>> pathEndingAtWikiUriByNoteId = new HashMap<>();
    if (focusId != null) {
      pathEndingAtWikiUriByNoteId.put(focusId, List.of(focusWikiUri));
    }

    List<Note> frontier = new ArrayList<>();
    frontier.add(hydrated);

    Set<Integer> wikiClaimedNoteIds = new HashSet<>();
    if (focusId != null) {
      wikiClaimedNoteIds.add(focusId);
    }

    List<SiblingAnchor> siblingAnchors = new ArrayList<>();
    siblingAnchors.add(new SiblingAnchor(hydrated, 0, List.of(focusWikiUri)));

    for (int depth = 1; depth <= config.getMaxDepth(); depth++) {
      if (wikiRemainingBudget <= 0 || frontier.isEmpty()) {
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
        if (wikiRemainingBudget <= 0) {
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
        int cost = Math.max(1, estimateTokens(details));
        wikiRemainingBudget -= cost;
        result.addRelatedNote(
            new FocusContextNote(
                hydratedNote.getNotebook() != null ? hydratedNote.getNotebook().getName() : null,
                hydratedNote.getTitle(),
                FolderTrailSegments.crumbPathJoinedBySlashSpace(hydratedNote),
                p.depth,
                p.retrievalPath,
                p.edgeType,
                null,
                hydratedNote.getCreatedAt(),
                details,
                truncated));
        wikiClaimedNoteIds.add(hydratedNote.getId());
        pathEndingAtWikiUriByNoteId.put(hydratedNote.getId(), p.retrievalPath);
        nextFrontier.add(hydratedNote);
        siblingAnchors.add(new SiblingAnchor(hydratedNote, p.depth, p.retrievalPath));
      }

      frontier = nextFrontier;
    }

    appendFolderSiblings(
        result,
        focusId,
        siblingAnchors,
        siblingBudgetTotal,
        wikiClaimedNoteIds,
        config.getFolderSiblingSampleSeed().orElse(null));

    return result;
  }

  private void appendFolderSiblings(
      FocusContextResult result,
      Integer focusId,
      List<SiblingAnchor> siblingAnchors,
      int siblingBudgetTokens,
      Set<Integer> wikiClaimedNoteIds,
      Long folderSiblingSampleSeed) {
    if (siblingBudgetTokens <= 0) {
      return;
    }

    Random rng = folderSiblingSampleSeed == null ? null : new Random(folderSiblingSampleSeed);

    List<SiblingOffer> offers = new ArrayList<>();
    int anchorIndex = 0;
    for (SiblingAnchor anchor : siblingAnchors) {
      List<Note> rawPeers = noteService.findStructuralPeerNotesInOrder(anchor.note);
      List<Note> candidates = new ArrayList<>();
      for (Note p : rawPeers) {
        if (p.getId() == null) {
          continue;
        }
        if (focusId != null && p.getId().equals(focusId)) {
          continue;
        }
        if (Objects.equals(p.getId(), anchor.note.getId())) {
          continue;
        }
        if (wikiClaimedNoteIds.contains(p.getId())) {
          continue;
        }
        candidates.add(p);
      }
      if (rng != null) {
        Collections.shuffle(candidates, rng);
      }
      int taken = 0;
      for (Note p : candidates) {
        if (taken >= FocusContextConstants.MAX_FOLDER_SIBLINGS_PER_NOTE) {
          break;
        }
        offers.add(
            new SiblingOffer(
                p.getId(),
                anchor.wikiDepth,
                anchorIndex,
                List.copyOf(anchor.pathToAnchorWikiUris)));
        taken++;
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
    if (idsToHydrate.isEmpty()) {
      return;
    }
    Map<Integer, Note> hydratedById = new LinkedHashMap<>();
    for (Note n : noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(idsToHydrate)) {
      hydratedById.put(n.getId(), n);
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
      String details =
          truncateToTokens(
              hydratedNote.getDetails(), FocusContextConstants.RELATED_NOTE_DETAILS_MAX_TOKENS);
      boolean truncated =
          details != null
              && hydratedNote.getDetails() != null
              && details.length() < hydratedNote.getDetails().length();
      int cost = Math.max(1, estimateTokens(details));
      if (cost > siblingRemaining) {
        continue;
      }
      siblingRemaining -= cost;
      result.addRelatedNote(
          new FocusContextNote(
              hydratedNote.getNotebook() != null ? hydratedNote.getNotebook().getName() : null,
              hydratedNote.getTitle(),
              FolderTrailSegments.crumbPathJoinedBySlashSpace(hydratedNote),
              o.anchorWikiDepth + 1,
              o.pathToAnchor,
              FocusContextEdgeType.FolderSibling,
              null,
              hydratedNote.getCreatedAt(),
              details,
              truncated));
    }
  }

  private record SiblingAnchor(Note note, int wikiDepth, List<String> pathToAnchorWikiUris) {}

  private record SiblingOffer(
      int noteId, int anchorWikiDepth, int anchorIndex, List<String> pathToAnchor) {}

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
    return edgePriority(candidate.edgeType) < edgePriority(existing.edgeType);
  }

  private static int edgePriority(FocusContextEdgeType t) {
    return switch (t) {
      case OutgoingWikiLink -> 0;
      case InboundWikiReference -> 1;
      case FolderSibling -> 2;
    };
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

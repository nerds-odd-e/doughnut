package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.WikiTitleCacheService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    List<Note> outgoingTargets =
        wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(hydrated, viewer);
    List<Note> inboundReferrers = wikiTitleCacheService.referencesNotesForViewer(hydrated, viewer);

    Map<Integer, Note> candidatesByNoteId = new LinkedHashMap<>();
    for (Note note : outgoingTargets) {
      candidatesByNoteId.put(note.getId(), note);
    }
    for (Note note : inboundReferrers) {
      if (!candidatesByNoteId.containsKey(note.getId())) {
        candidatesByNoteId.put(note.getId(), note);
      }
    }

    List<Integer> ids = new ArrayList<>(candidatesByNoteId.keySet());
    List<Note> hydratedNotes =
        ids.isEmpty()
            ? List.of()
            : noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(ids);

    Map<Integer, Note> hydratedById = new LinkedHashMap<>();
    for (Note note : hydratedNotes) {
      hydratedById.put(note.getId(), note);
    }

    int remainingBudget = FocusContextConstants.RELATED_NOTES_TOTAL_BUDGET_TOKENS;
    String focusWikiUri = FocusContextWikiUri.ofFocusNote(hydrated);

    for (Note outgoing : outgoingTargets) {
      Note hydratedNote = hydratedById.get(outgoing.getId());
      if (hydratedNote == null) continue;
      if (remainingBudget <= 0) break;
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
              1,
              List.of(focusWikiUri, FocusContextWikiUri.of(hydratedNote)),
              FocusContextEdgeType.OutgoingWikiLink,
              details,
              truncated));
    }

    for (Note referrer : inboundReferrers) {
      if (candidatesByNoteId.get(referrer.getId()) == null) continue;
      boolean alreadyAdded =
          outgoingTargets.stream().anyMatch(o -> o.getId().equals(referrer.getId()));
      if (alreadyAdded) continue;
      Note hydratedNote = hydratedById.get(referrer.getId());
      if (hydratedNote == null) continue;
      if (remainingBudget <= 0) break;
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
              1,
              List.of(focusWikiUri, FocusContextWikiUri.of(hydratedNote)),
              FocusContextEdgeType.InboundWikiReference,
              details,
              truncated));
    }

    return result;
  }

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

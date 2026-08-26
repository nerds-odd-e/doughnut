package com.odde.donut.services.focusContext;

import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.ApproximateUtf8TokenBudget;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.WikiTitleCacheService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FocusContextRetrievalService {

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final NoteService noteService;
  private final FocusContextWikiBfsExpander wikiBfsExpander;
  private final FocusContextFolderPeerAppender folderPeerAppender;

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
    this.wikiBfsExpander = new FocusContextWikiBfsExpander(wikiTitleCacheService, noteRepository);
    this.folderPeerAppender = new FocusContextFolderPeerAppender(noteRepository, noteService);
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

    int combinedContentBudget = config.getFocusContextContentTokenBudget();
    int focusDetailTruncationCap =
        Math.min(
            FocusContextConstants.FOCUS_NOTE_CONTENT_MAX_TOKENS,
            Math.max(0, combinedContentBudget));
    String focusDetails =
        ApproximateUtf8TokenBudget.truncateByApproxTokens(
            hydrated.getContent(), focusDetailTruncationCap);
    boolean focusTruncated =
        focusDetails != null
            && hydrated.getContent() != null
            && focusDetails.length() < hydrated.getContent().length();

    Integer focusId = hydrated.getId();

    List<String> outgoingLinkUris =
        wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(hydrated, viewer).stream()
            .map(FocusContextWikiUri::of)
            .toList();
    Set<Integer> focusInboundExclude = new HashSet<>();
    if (focusId != null) {
      focusInboundExclude.add(focusId);
    }
    List<String> inboundRefUris =
        wikiTitleCacheService
            .sampledReferencesNotesForFocusContext(
                hydrated,
                viewer,
                focusInboundExclude,
                FocusContextConstants.FOCUS_INBOUND_URI_CAP,
                config.getSampleSeed())
            .stream()
            .map(FocusContextWikiUri::of)
            .toList();
    int focusContentTokens = ApproximateUtf8TokenBudget.estimateApproxTokens(focusDetails);
    int relatedTotalBudget = Math.max(0, combinedContentBudget - focusContentTokens);
    boolean includeFolderPeers =
        relatedTotalBudget >= FocusContextConstants.MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT;

    List<String> sampleSiblingUris =
        includeFolderPeers
            ? noteService
                .findStructuralPeerNotesSample(
                    hydrated,
                    focusId,
                    Set.of(),
                    FocusContextConstants.sampleCapAtGraphDepth(1),
                    config.getSampleSeed())
                .stream()
                .map(FocusContextWikiUri::of)
                .toList()
            : List.of();

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

    if (config.getMaxDepth() < 1 || relatedTotalBudget <= 0) {
      return result;
    }

    int wikiBudgetTotal;
    int siblingBudgetTotal;
    if (includeFolderPeers) {
      wikiBudgetTotal =
          (int)
              Math.floor(
                  relatedTotalBudget * FocusContextConstants.RELATED_NOTES_WIKI_BUDGET_FRACTION);
      siblingBudgetTotal = relatedTotalBudget - wikiBudgetTotal;
    } else {
      wikiBudgetTotal = relatedTotalBudget;
      siblingBudgetTotal = 0;
    }

    var wikiExpansion =
        wikiBfsExpander.expand(
            result,
            hydrated,
            focusId,
            viewer,
            config.getMaxDepth(),
            wikiBudgetTotal,
            config.getSampleSeed());

    folderPeerAppender.append(
        result,
        focusId,
        wikiExpansion.siblingAnchors(),
        siblingBudgetTotal,
        wikiExpansion.wikiClaimedNoteIds(),
        config.getSampleSeed());

    return result;
  }
}

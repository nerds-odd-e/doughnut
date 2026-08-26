package com.odde.donut.services;

import com.odde.donut.controllers.dto.LearningSessionRequestResponse;
import com.odde.donut.controllers.dto.RecordLearningSessionResponse;
import com.odde.donut.controllers.dto.RecordedLearningSessionItem;
import com.odde.donut.entities.*;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.LearningSessionReportParser.ParseResult;
import com.odde.donut.services.LearningSessionReportParser.ParsedReportEntry;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningSessionService {

  private final UserService userService;
  private final NoteRepository noteRepository;
  private final LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder;
  private final LearningSessionReportParser learningSessionReportParser;
  private final MemoryTrackerService memoryTrackerService;

  @Autowired
  public LearningSessionService(
      UserService userService,
      NoteRepository noteRepository,
      LearningSessionRequestMarkdownBuilder learningSessionRequestMarkdownBuilder,
      LearningSessionReportParser learningSessionReportParser,
      MemoryTrackerService memoryTrackerService) {
    this.userService = userService;
    this.noteRepository = noteRepository;
    this.learningSessionRequestMarkdownBuilder = learningSessionRequestMarkdownBuilder;
    this.learningSessionReportParser = learningSessionReportParser;
    this.memoryTrackerService = memoryTrackerService;
  }

  @Transactional(readOnly = true)
  public LearningSessionRequestResponse request(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers =
        requireDueCommissionedNoteLevelTrackers(user, notebook, now, zoneId);

    LearningSessionRequestResponse response = new LearningSessionRequestResponse();
    response.setRequestMarkdown(
        learningSessionRequestMarkdownBuilder.build(user, notebook, dueTrackers, zoneId));
    return response;
  }

  @Transactional
  public RecordLearningSessionResponse record(
      User user, Notebook notebook, String reportMarkdown, Timestamp now) {
    List<Note> notebookNotes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    List<String> notebookTitleList = notebookNotes.stream().map(Note::getTitle).toList();
    Set<String> notebookTitles = Set.copyOf(notebookTitleList);
    Set<String> ambiguousTitles = LearningSessionReportParser.ambiguousTitles(notebookTitleList);
    ParseResult parseResult =
        learningSessionReportParser.parse(reportMarkdown, notebookTitles, ambiguousTitles);

    RecordLearningSessionResponse response = new RecordLearningSessionResponse();
    response.setRecordedItems(new ArrayList<>());
    response.setRejectedEntries(
        new ArrayList<>(
            RejectedLearningSessionReportEntryMapper.fromParsed(parseResult.rejected())));

    List<MatchedReportEntry> matchedEntries = new ArrayList<>();
    for (ParsedReportEntry entry : parseResult.entries()) {
      Optional<MemoryTracker> tracker =
          findCommissionedNoteLevelTracker(user, notebookNotes, entry.noteTitle());
      if (tracker.isEmpty()) {
        response
            .getRejectedEntries()
            .add(
                RejectedLearningSessionReportEntryMapper.of(
                    entry.noteTitle() + ": " + entry.grade().getValue(),
                    "No commissioned memory tracker for this note."));
        continue;
      }
      matchedEntries.add(new MatchedReportEntry(entry, tracker.get()));
    }

    if (matchedEntries.isEmpty()) {
      return response;
    }

    for (MatchedReportEntry matched : matchedEntries) {
      Grade grade = matched.entry().grade();
      memoryTrackerService.markAsRecalled(
          now, grade, matched.tracker(), null, matched.entry().descriptiveText());

      RecordedLearningSessionItem recorded = new RecordedLearningSessionItem();
      recorded.setNoteTitle(matched.entry().noteTitle());
      recorded.setGrade(grade.getValue());
      recorded.setMemoryTrackerId(matched.tracker().getId());
      response.getRecordedItems().add(recorded);
    }

    response.setRecordedAt(now);
    return response;
  }

  private Optional<MemoryTracker> findCommissionedNoteLevelTracker(
      User user, List<Note> notebookNotes, String noteTitle) {
    return notebookNotes.stream()
        .filter(note -> note.getTitle().equals(noteTitle))
        .flatMap(note -> userService.getMemoryTrackersFor(user, note).stream())
        .filter(MemoryTracker::isCommissioned)
        .filter(MemoryTracker::isNoteLevelTracker)
        .findFirst();
  }

  private List<MemoryTracker> requireDueCommissionedNoteLevelTrackers(
      User user, Notebook notebook, Timestamp now, ZoneId zoneId) {
    List<MemoryTracker> dueTrackers =
        userService
            .getCommissionedMemoryTrackersNeedToRepeat(user, now, zoneId)
            .filter(tracker -> tracker.getNote().getNotebook().getId().equals(notebook.getId()))
            .filter(MemoryTracker::isNoteLevelTracker)
            .toList();
    if (dueTrackers.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No due commissioned memory trackers for this notebook.");
    }
    return dueTrackers;
  }

  private record MatchedReportEntry(ParsedReportEntry entry, MemoryTracker tracker) {}
}

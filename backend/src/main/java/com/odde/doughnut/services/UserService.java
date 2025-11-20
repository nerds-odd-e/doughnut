package com.odde.doughnut.services;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteReviewRepository;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final NoteReviewRepository noteReviewRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityManager entityManager;

  public UserService(
      NoteReviewRepository noteReviewRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      EntityManager entityManager) {
    this.noteReviewRepository = noteReviewRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityManager = entityManager;
  }

  public void setDailyAssimilationCount(User user, Integer count) {
    user.setDailyAssimilationCount(count);
    entityManager.merge(user);
  }

  public void setSpaceIntervals(User user, String spaceIntervals) {
    user.setSpaceIntervals(spaceIntervals);
    entityManager.merge(user);
  }

  public int getUnassimilatedNoteCount(User user) {
    return noteReviewRepository.countByOwnershipWhereThereIsNoMemoryTracker(
        user.getId(), user.getOwnership().getId());
  }

  public Stream<Note> getUnassimilatedNotes(User user) {
    return noteReviewRepository.findByOwnershipWhereThereIsNoMemoryTracker(
        user.getId(), user.getOwnership().getId());
  }

  public List<MemoryTracker> getRecentMemoryTrackers(User user, Timestamp since) {
    return memoryTrackerRepository.findAllByUserAndAssimilatedAtGreaterThan(user, since);
  }

  public Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
    return memoryTrackerRepository.findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
        user.getId(), timestamp);
  }

  public List<MemoryTracker> getMemoryTrackersFor(User user, Note note) {
    if (user == null) return List.of();
    return memoryTrackerRepository.findByUserAndNote(user.getId(), note.getId());
  }
}

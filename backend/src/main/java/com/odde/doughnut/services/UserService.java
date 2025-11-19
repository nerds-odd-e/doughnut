package com.odde.doughnut.services;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteReviewRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
  private final AuthorizationService authorizationService;
  private final EntityManager entityManager;

  public UserService(
      NoteReviewRepository noteReviewRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      AuthorizationService authorizationService,
      EntityManager entityManager) {
    this.noteReviewRepository = noteReviewRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.authorizationService = authorizationService;
    this.entityManager = entityManager;
  }

  public String getName(User user) {
    return user.getName();
  }

  public void setDailyAssimilationCount(User user, Integer dailyAssimilationCount) {
    user.setDailyAssimilationCount(dailyAssimilationCount);
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
    return memoryTrackerRepository.findAllByUserAndAssimilatedAtGreaterThan(user.getId(), since);
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

  public <T> void assertAuthorization(User user, T object) throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(user, object);
  }

  public <T> void assertReadAuthorization(User user, T object)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(user, object);
  }

  public void assertAdminAuthorization(User user) throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization(user);
  }

  public void assertLoggedIn(User user) {
    authorizationService.assertLoggedIn(user);
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteReviewRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final NoteReviewRepository noteReviewRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityPersister entityPersister;
  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;

  public UserService(
      NoteReviewRepository noteReviewRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      EntityPersister entityPersister,
      UserRepository userRepository,
      UserTokenRepository userTokenRepository) {
    this.noteReviewRepository = noteReviewRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
    this.userRepository = userRepository;
    this.userTokenRepository = userTokenRepository;
  }

  public void setDailyAssimilationCount(User user, Integer count) {
    user.setDailyAssimilationCount(count);
    entityPersister.merge(user);
  }

  public void setSpaceIntervals(User user, String spaceIntervals) {
    user.setSpaceIntervals(spaceIntervals);
    entityPersister.merge(user);
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

  public Optional<User> findUserByToken(String token) {
    UserToken usertoken = userTokenRepository.findByToken(token);

    if (usertoken == null) {
      AuthorizationService.throwUserNotFound();
    }

    return userRepository.findById(usertoken.getUserId());
  }

  public Optional<List<UserToken>> findTokensByUser(Integer id) {
    List<UserToken> usertokens = userTokenRepository.findByUserId(id);
    return Optional.ofNullable(usertokens);
  }

  public Optional<UserToken> findTokenByTokenId(Integer id) {
    return userTokenRepository.findById(id);
  }

  public void deleteToken(Integer tokenId) {
    userTokenRepository.deleteById(tokenId);
  }
}

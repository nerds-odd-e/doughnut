package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

public class UserModel implements ReviewScope {

  @Getter protected final User entity;
  protected final ModelFactoryService modelFactoryService;

  public UserModel(User user, ModelFactoryService modelFactoryService) {
    this.entity = user;
    this.modelFactoryService = modelFactoryService;
  }

  public Authorization getAuthorization() {
    return modelFactoryService.toAuthorization(entity);
  }

  public boolean loggedIn() {
    return entity != null;
  }

  public String getName() {
    return entity.getName();
  }

  public void setAndSaveDailyNewNotesCount(Integer dailyNewNotesCount) {
    entity.setDailyNewNotesCount(dailyNewNotesCount);
    save();
  }

  public void setAndSaveSpaceIntervals(String spaceIntervals) {
    entity.setSpaceIntervals(spaceIntervals);
    save();
  }

  @Override
  public Stream<Note> getNotesHaveNotBeenReviewedAtAll() {
    return modelFactoryService.noteRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  @Override
  public int getNotesHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.noteRepository.countByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  @Override
  public Stream<Link> getLinksHaveNotBeenReviewedAtAll() {
    return modelFactoryService.linkRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  @Override
  public int getLinksHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.linkRepository.countByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  public List<ReviewPoint> getRecentReviewPoints(Timestamp since) {
    return modelFactoryService.reviewPointRepository.findAllByUserAndInitialReviewedAtGreaterThan(
        entity, since);
  }

  public List<ReviewPoint> getReviewPointsNeedToRepeat(Timestamp currentUTCTimestamp) {
    final ZoneId timeZone = getTimeZone();
    final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
    return modelFactoryService.reviewPointRepository
        .findAllByUserAndNextReviewAtLessThanEqualOrderByNextReviewAt(getEntity(), timestamp);
  }

  int learntCount() {
    return modelFactoryService.reviewPointRepository.countByUserNotRemoved(entity);
  }

  public Reviewing createReviewing(Timestamp currentUTCTimestamp) {
    return new Reviewing(this, currentUTCTimestamp, modelFactoryService);
  }

  private void save() {
    modelFactoryService.entityManager.persist(entity);
  }

  boolean isInitialReviewOnSameDay(ReviewPoint reviewPoint, Timestamp currentUTCTimestamp) {
    return reviewPoint.isInitialReviewOnSameDay(currentUTCTimestamp, getTimeZone());
  }

  public ZoneId getTimeZone() {
    return ZoneId.of("Asia/Shanghai");
  }

  public ReviewPoint getReviewPointFor(Note note) {
    return modelFactoryService.reviewPointRepository.findByUserAndNote(entity, note);
  }

  public ReviewPoint getReviewPointFor(Link link) {
    return modelFactoryService.reviewPointRepository.findByUserAndLink(entity, link);
  }
}

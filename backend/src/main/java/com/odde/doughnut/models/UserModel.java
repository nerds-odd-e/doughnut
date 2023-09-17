package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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

  private Authorization getAuthorization() {
    return modelFactoryService.toAuthorization(entity);
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
  public int getThingsHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.thingRepository.countByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  @Override
  public Stream<Thing> getThingHaveNotBeenReviewedAtAll() {
    return modelFactoryService.thingRepository.findByOwnershipWhereThereIsNoReviewPoint(entity);
  }

  public List<ReviewPoint> getRecentReviewPoints(Timestamp since) {
    return modelFactoryService.reviewPointRepository.findAllByUserAndInitialReviewedAtGreaterThan(
        entity, since);
  }

  public Stream<ReviewPoint> getReviewPointsNeedToRepeat(
      Timestamp currentUTCTimestamp, ZoneId timeZone) {
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
    return getReviewPointFor(note.getThing());
  }

  public ReviewPoint getReviewPointFor(Thing thing) {
    return modelFactoryService.reviewPointRepository.findByUserAndThing(entity, thing);
  }

  public <T> void assertAuthorization(T object) throws UnexpectedNoAccessRightException {
    getAuthorization().assertAuthorization(object);
  }

  public <T> void assertReadAuthorization(T object) throws UnexpectedNoAccessRightException {
    getAuthorization().assertReadAuthorization(object);
  }

  public void assertAdminAuthorization() throws UnexpectedNoAccessRightException {
    getAuthorization().assertAdminAuthorization();
  }

  public void assertLoggedIn() {
    getAuthorization().assertLoggedIn();
  }
}

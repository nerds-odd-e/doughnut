package com.odde.donut.services;

import com.odde.donut.entities.Circle;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.CircleRepository;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class CircleService {
  private final CircleRepository circleRepository;
  private final EntityPersister entityPersister;

  public CircleService(CircleRepository circleRepository, EntityPersister entityPersister) {
    this.circleRepository = circleRepository;
    this.entityPersister = entityPersister;
  }

  public void joinAndSave(Circle circle, User user) {
    circle.getMembers().add(user);
    entityPersister.save(circle);
  }

  public Circle findCircleByInvitationCode(String invitationCode) {
    return circleRepository.findFirstByInvitationCode(invitationCode);
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.CircleRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
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

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.CircleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class CircleService {
  private final CircleRepository circleRepository;
  private final EntityManager entityManager;

  public CircleService(CircleRepository circleRepository, EntityManager entityManager) {
    this.circleRepository = circleRepository;
    this.entityManager = entityManager;
  }

  public void joinAndSave(Circle circle, User user) {
    circle.getMembers().add(user);
    if (circle.getId() == null) {
      entityManager.persist(circle);
    } else {
      entityManager.merge(circle);
    }
  }

  public Circle findCircleByInvitationCode(String invitationCode) {
    return circleRepository.findFirstByInvitationCode(invitationCode);
  }
}

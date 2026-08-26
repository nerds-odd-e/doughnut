package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Circle;
import org.springframework.data.repository.CrudRepository;

public interface CircleRepository extends CrudRepository<Circle, Integer> {
  Circle findFirstByInvitationCode(String invitationCode);

  Circle findByName(String circleName);
}

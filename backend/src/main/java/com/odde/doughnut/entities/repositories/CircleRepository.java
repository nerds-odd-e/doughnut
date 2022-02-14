package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Circle;
import org.springframework.data.repository.CrudRepository;

public interface CircleRepository extends CrudRepository<Circle, Integer> {
    Circle findFirstByInvitationCode(String invitationCode);

    Circle findByName(String circleName);
}

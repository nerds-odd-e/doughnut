package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends CrudRepository<SubscriptionEntity, Integer> {
}

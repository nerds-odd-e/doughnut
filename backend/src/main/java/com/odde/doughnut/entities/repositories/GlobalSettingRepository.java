package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.GlobalSettings;
import org.springframework.data.repository.CrudRepository;

public interface GlobalSettingRepository extends CrudRepository<GlobalSettings, Integer> {
  GlobalSettings findByKeyName(String keyName);
}

package com.odde.donut.entities.repositories;

import com.odde.donut.entities.GlobalSettings;
import org.springframework.data.repository.CrudRepository;

public interface GlobalSettingRepository extends CrudRepository<GlobalSettings, Integer> {
  GlobalSettings findByKeyName(String keyName);
}

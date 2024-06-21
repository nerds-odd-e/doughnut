package com.odde.doughnut.services;

import java.sql.Timestamp;

public interface SettingAccessor {
  String getValue();

  void setKeyValue(Timestamp _currentTime, String value);
}

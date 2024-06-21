package com.odde.doughnut.testability.model;

import com.odde.doughnut.services.SettingAccessor;
import java.sql.Timestamp;

public class MemorySettingAccessor implements SettingAccessor {
  private String value;

  public MemorySettingAccessor(String initialValue) {
    this.value = initialValue;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setKeyValue(Timestamp _currentTime, String value) {
    this.value = value;
  }
}

package com.odde.doughnut.testability.model;

import com.odde.doughnut.services.GlobalSettingsService.GlobalSettingsKeyValue;
import java.sql.Timestamp;

public class MemorySettingAccessor extends GlobalSettingsKeyValue {
  private String value;

  public MemorySettingAccessor(String initialValue) {
    super(null, null, null);
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

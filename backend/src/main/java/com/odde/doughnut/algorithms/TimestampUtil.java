package com.odde.doughnut.algorithms;

import java.sql.Timestamp;
import java.util.Calendar;

public class TimestampUtil {
  public static Timestamp addYearsToTimestamp(Timestamp ts) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(ts);
    cal.add(Calendar.YEAR, 1);
    ts.setTime(cal.getTime().getTime());
    return ts;
  }
}

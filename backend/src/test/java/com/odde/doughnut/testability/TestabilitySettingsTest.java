package com.odde.doughnut.testability;

import org.junit.jupiter.api.Test;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestabilitySettingsTest {
    private TestabilitySettings testabilitySettings = new TestabilitySettings();

    @Test
    void shouldResponseTrueIfDateIsBefore_isDateBefore() {
        Date date1 = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        calendar.add(Calendar.SECOND, -1);

        boolean response = testabilitySettings.isDateBefore(calendar.getTime(), new Date());

        assertThat(response, equalTo(true));
    }

    @Test
    void shouldResponseFalseIfDateIsSame_isDateBefore() {
        Date date = new Date();
        boolean response = testabilitySettings.isDateBefore(date, date);
        assertThat(response, equalTo(false));
    }

    @Test
    void shouldResponseFalseIfDateIsAfter_isDateBefore() {
        Date date1 = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        calendar.add(Calendar.SECOND, +1);

        boolean response = testabilitySettings.isDateBefore(calendar.getTime(), new Date());

        assertThat(response, equalTo(false));
    }

    @Test
    void shouldResponseFalseIfDateToCheckIsNull_isDateBefore() {
        Date date = new Date();
        boolean response = testabilitySettings.isDateBefore(null, date);
        assertThat(response, equalTo(false));
    }

    @Test
    void shouldResponseFalseIfDateExistsIsNull_isDateBefore() {
        Date date = new Date();
        boolean response = testabilitySettings.isDateBefore(date, null);
        assertThat(response, equalTo(false));
    }

    @Test
    void shouldResponseFalseIfDateIsNotBefore_isNotBefore() {
        Date date1 = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        calendar.add(Calendar.SECOND, -1);

        boolean response = testabilitySettings.isDateNotBefore(calendar.getTime(), new Date());

        assertThat(response, equalTo(false));
    }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import com.odde.donut.controllers.dto.RecallHistoryExport;
import com.odde.donut.controllers.dto.RecallHistoryExport.DailyProbe;
import com.odde.donut.controllers.dto.UserDTO;
import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

class UserRecallHistoryExportControllerTest extends UserRecallHistoryExportControllerTestBase {
  @Autowired MockMvc mockMvc;
  @Autowired DailyProbeController dailyProbeController;

  @Test
  void requiresLogin() {
    currentUser.setUser(null);
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.exportRecallHistory("Asia/Bangkok"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void namesTheTimezoneAndTheExportTimeInThatZone() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 8).please());

    RecallHistoryExport export = controller.exportRecallHistory("Asia/Bangkok");

    assertThat(export.timezone(), equalTo("Asia/Bangkok"));
    assertThat(export.exportedAt(), equalTo("1989-01-02T15:00:00+07:00"));
    assertThat(export.recallEvents(), hasSize(0));
  }

  @Test
  void anAgentWithTheLearnersTokenGetsTheJson() throws Exception {
    answerMcqAtDay(1);
    takeProbeAtDay(2, 3.5, 85, 2, 0.4);
    UserToken token =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("agent").please();

    mockMvc
        .perform(
            get("/api/user/recall-history-export")
                .param("timezone", "Asia/Bangkok")
                .header("Authorization", "Bearer " + token.getToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("Asia/Bangkok"))
        .andExpect(jsonPath("$.recallEvents[0].recordedAt").value("1989-01-02T15:00:00+07:00"))
        .andExpect(jsonPath("$.recallEvents[0].kind").value("ANSWERED_RECALL_PROMPT"))
        .andExpect(jsonPath("$.recallEvents[0].answer.questionType").value("MCQ"))
        .andExpect(
            jsonPath("$.recallEvents[0].answer.createdAt").value("1989-01-02T15:00:00+07:00"))
        .andExpect(jsonPath("$.dailyProbes[0].completedAt").value("1989-01-03T15:00:00+07:00"))
        .andExpect(jsonPath("$.dailyProbes[0].accuracy").value(85))
        .andExpect(jsonPath("$.dailyProbes[0].trials[0].rtMs").value(612));
  }

  @Test
  void aRequestWithoutCredentialIsRefused() throws Exception {
    currentUser.setUser(null);

    mockMvc
        .perform(get("/api/user/recall-history-export").param("timezone", "Asia/Bangkok"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials() throws Exception {
    takeProbeAtDay(3, 3.5, 85, 2, 0.4);
    takeProbeAtDay(1, 4.0, 100, 0, 0.1);
    makeMe.aDailyProbe().by(makeMe.aUser().please()).please();

    List<DailyProbe> probes = controller.exportRecallHistory("Asia/Bangkok").dailyProbes();

    assertThat(
        probes.stream().map(DailyProbe::completedAt).toList(),
        contains("1989-01-02T15:00:00+07:00", "1989-01-04T15:00:00+07:00"));
    DailyProbe later = probes.get(1);
    assertThat(
        List.of(later.speed(), later.accuracy(), later.lapseCount(), later.variability()),
        contains(3.5, 85, 2, 0.4));
    assertThat(later.trials(), hasSize(20));
    DailyProbeTrialDTO trial = later.trials().get(0);
    assertThat(
        List.of(trial.stimulus, trial.response, trial.rtMs, trial.correct),
        contains("left", "right", 612, false));
  }

  @Test
  void probesTakenBeforeTheProbeWasTurnedOffAreStillExported() throws Exception {
    User user = currentUser.getUser();
    setDailyProbeEnabled(user, true);
    takeProbeAtDay(1, 4.0, 100, 0, 0.1);
    setDailyProbeEnabled(user, false);

    List<DailyProbe> probes = controller.exportRecallHistory("Asia/Bangkok").dailyProbes();

    assertThat(
        probes.stream().map(DailyProbe::completedAt).toList(),
        contains("1989-01-02T15:00:00+07:00"));
  }

  @Test
  void aLearnerWithoutProbesGetsRecallEventsAndAnEmptyProbeList()
      throws UnexpectedNoAccessRightException {
    answerMcqAtDay(1);

    RecallHistoryExport export = controller.exportRecallHistory("Asia/Bangkok");

    assertThat(export.recallEvents(), hasSize(1));
    assertThat(export.dailyProbes(), hasSize(0));
  }

  private void takeProbeAtDay(
      int day, double speed, int accuracy, int lapseCount, double variability) throws Exception {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(day, 8).please());
    DailyProbeRequestDTO request = makeMe.aDailyProbe().createRequest();
    request.speed = speed;
    request.accuracy = accuracy;
    request.lapseCount = lapseCount;
    request.variability = variability;
    DailyProbeTrialDTO first = request.trials.get(0);
    first.response = "right";
    first.rtMs = 612;
    first.correct = false;
    dailyProbeController.createDailyProbe(request);
  }

  private void setDailyProbeEnabled(User user, boolean enabled)
      throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName(user.getName());
    dto.setDailyAssimilationCount(user.getDailyAssimilationCount());
    dto.setDailyProbeEnabled(enabled);
    controller.updateUser(user, dto);
  }
}

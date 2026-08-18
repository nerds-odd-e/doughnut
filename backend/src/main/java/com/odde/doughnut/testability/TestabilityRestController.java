package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.CircleService;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.model.McqsTestData;
import com.odde.doughnut.testability.model.NotesTestData;
import com.odde.doughnut.utils.TimestampOperations;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"e2e", "test"})
@RequestMapping("/api/testability")
class TestabilityRestController {

  private static final Object E2E_DB_RESET_LOCK = new Object();

  @PersistenceContext EntityManager entityManager;
  @Autowired NoteRepository noteRepository;
  @Autowired NotebookRepository notebookRepository;
  @Autowired UserRepository userRepository;
  @Autowired EntityPersister entityPersister;
  @Autowired CircleService circleService;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired BazaarService bazaarService;
  @Autowired UserService userService;
  @Autowired InjectNotesWorker injectNotesWorker;

  @PostMapping("/clean_db_and_reset_testability_settings")
  @Transactional
  public String resetDBAndTestabilitySettings() {
    synchronized (E2E_DB_RESET_LOCK) {
      new DBCleanerWorker().truncateAllTables(entityManager);
      createUser("old_learner", "Old Learner");
      createUser("another_old_learner", "Another Old Learner");
      createUser("admin", "admin");
      createUser("non_admin", "Non Admin");
      createUser("a_trainer", "A Trainer");
      testabilitySettings.init();
    }
    return "OK";
  }

  @PostMapping("/feature_toggle")
  @Transactional
  public List enableFeatureToggle(@RequestBody Map<String, String> requestBody) {
    testabilitySettings.enableFeatureToggle(requestBody.get("enabled").equals("true"));
    return new ArrayList();
  }

  @GetMapping("/feature_toggle")
  public Boolean getFeatureToggle() {
    return testabilitySettings.isFeatureToggleEnabled();
  }

  private void createUser(String externalIdentifier, String name) {
    User user = new User();
    user.setExternalIdentifier(externalIdentifier);
    user.setName(name);
    entityPersister.save(user);
  }

  @Schema(name = "ShareToBazaarRequest")
  @Getter
  @Setter
  static class ShareToBazaarRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;
  }

  @PostMapping("/inject_notes")
  @Transactional
  public Map<String, Integer> injectNotes(@RequestBody NotesTestData notesTestData) {
    if (Strings.isEmpty(notesTestData.getExternalIdentifier())) {
      throw new RuntimeException("externalIdentifier is required and cannot be empty");
    }
    User user = getUserModelByExternalIdentifier(notesTestData.getExternalIdentifier());
    return injectNotesWorker.inject(notesTestData, user);
  }

  @PostMapping("/inject-mcqs")
  @Transactional
  public List<Mcq> injectMcq(@RequestBody McqsTestData mcqsTestData) {
    List<Mcq> mcqs = mcqsTestData.buildMcqs(this.noteRepository);
    mcqs.forEach(question -> entityPersister.save(question));
    return mcqs;
  }

  @PostMapping("/share_to_bazaar")
  @Transactional
  public String shareToBazaar(@RequestBody ShareToBazaarRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    Notebook notebook =
        notebookRepository
            .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(
                new DisplayName(request.getNotebookName()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No notebook with name: " + request.getNotebookName()));
    bazaarService.shareNotebook(notebook);
    return "OK";
  }

  @PostMapping("/testability_update_user")
  @Transactional
  public String testabilityUpdateUser(
      @RequestParam String username, @RequestBody HashMap<String, String> userInfo) {
    User user = getUserModelByExternalIdentifier(username);
    if (userInfo.containsKey("daily_assimilation_count")) {
      userService.setDailyAssimilationCount(
          user, Integer.valueOf(userInfo.get("daily_assimilation_count")));
    }
    return "OK";
  }

  @PostMapping("/inject_circle")
  @Transactional
  public String injectCircle(@RequestBody HashMap<String, String> circleInfo) {
    Circle entity = new Circle();
    entity.setName(circleInfo.get("circleName"));
    entityPersister.save(entity);
    Arrays.stream(circleInfo.get("members").split(","))
        .map(String::trim)
        .forEach(
            s -> {
              circleService.joinAndSave(entity, getUserModelByExternalIdentifier(s));
            });
    entityPersister.flush();
    return entity.getId() + "," + entity.getInvitationCode();
  }

  private User getUserModelByExternalIdentifier(String externalIdentifier) {
    User user = userRepository.findByExternalIdentifier(externalIdentifier);
    if (user == null) {
      throw new RuntimeException(
          "User with external identifier `" + externalIdentifier + "` does not exist");
    }
    return user;
  }

  static DateTimeFormatter getDateTimeFormatter() {
    String pattern = "\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"";
    return DateTimeFormatter.ofPattern(pattern);
  }

  @PostMapping("/trigger_exception")
  public String triggerException(Model model) {
    throw new RuntimeException("for failure report");
  }

  @PostMapping("/use_real_sandbox_github_and_close_all_github_issues")
  public String closeAllGithubIssues(Model model) throws IOException, InterruptedException {
    testabilitySettings.setUseRealGithub(true);
    getGithubService().closeAllOpenIssues();
    return "OK";
  }

  @GetMapping("/github_issues")
  public List<Map<String, Object>> githubIssues() throws IOException, InterruptedException {
    return getGithubService().getOpenIssues();
  }

  private GithubService getGithubService() {
    return testabilitySettings.getGithubService();
  }

  static class TimeTravel {
    public String travel_to;
  }

  @PostMapping(value = "/time_travel")
  public List<Object> timeTravel(@RequestBody TimeTravel timeTravel) {
    DateTimeFormatter formatter = TestabilityRestController.getDateTimeFormatter();
    LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(timeTravel.travel_to));
    Timestamp timestamp = Timestamp.valueOf(localDateTime);
    testabilitySettings.timeTravelTo(timestamp);
    return Collections.emptyList();
  }

  static class TimeTravelRelativeToNow {
    public Integer hours;
  }

  @PostMapping(value = "/time_travel_relative_to_now")
  public List<Object> timeTravelRelativeToNow(
      @RequestBody TimeTravelRelativeToNow timeTravelRelativeToNow) {
    Timestamp timestamp =
        TimestampOperations.addHoursToTimestamp(
            new Timestamp(System.currentTimeMillis()), timeTravelRelativeToNow.hours);
    testabilitySettings.timeTravelTo(timestamp);
    return Collections.emptyList();
  }

  @PostMapping(value = "/replace_service_url")
  public void replaceServiceUrl(@RequestBody Map<String, String> setWikidataService) {
    testabilitySettings.replaceServiceUrls(setWikidataService);
  }

  @PostMapping(value = "/open_ai_token")
  public void setOpenAiToken(@RequestBody(required = false) Map<String, String> body) {
    String token = body != null ? body.get("token") : null;
    testabilitySettings.setOpenAiTokenOverride(token);
  }

  @PostMapping(value = "/randomizer")
  public List<Object> randomizer(@RequestBody Randomization randomization) {
    testabilitySettings.setRandomization(randomization);
    return Collections.emptyList();
  }
}

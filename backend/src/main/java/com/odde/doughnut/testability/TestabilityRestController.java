package com.odde.doughnut.testability;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.CircleService;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.SuggestedQuestionForFineTuningService;
import com.odde.doughnut.testability.model.PredefinedQuestionsTestData;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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

  @Autowired EntityManagerFactory emf;
  @Autowired NoteRepository noteRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserModel currentUser;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired CircleService circleService;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;
  @Autowired BazaarService bazaarService;

  @PostMapping("/clean_db_and_reset_testability_settings")
  @Transactional
  public String resetDBAndTestabilitySettings() {
    new DBCleanerWorker(emf).truncateAllTables();
    createUser("old_learner", "Old Learner");
    createUser("another_old_learner", "Another Old Learner");
    createUser("admin", "admin");
    createUser("non_admin", "Non Admin");
    createUser("a_trainer", "A Trainer");
    testabilitySettings.init();
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
    modelFactoryService.save(user);
  }

  static class NoteTestData {
    @JsonProperty("Title")
    public String title;

    @JsonProperty("Details")
    @Setter
    private String details;

    @JsonProperty("Parent Title")
    @Setter
    private String parentTitle;

    @JsonProperty("Skip Memory Tracking")
    @Setter
    private Boolean skipMemoryTracking;

    @JsonProperty("Image Url")
    @Setter
    private String imageUrl;

    @JsonProperty("Image Mask")
    @Setter
    private String imageMask;

    @JsonProperty("Wikidata Id")
    @Setter
    private String wikidataId;

    private Note buildNote(User user, Timestamp currentUTCTimestamp) {
      Note note =
          new NoteConstructionService(user, currentUTCTimestamp, null, null)
              .createNote(null, title);
      NoteAccessory content = note.getOrInitializeNoteAccessory();

      note.setTopicConstructor(title);
      note.setDetails(details);
      note.setUpdatedAt(currentUTCTimestamp);
      if (skipMemoryTracking != null) {
        note.getRecallSetting().setSkipMemoryTracking(skipMemoryTracking);
      }
      content.setImageMask(imageMask);
      content.setImageUrl(imageUrl);

      note.setWikidataId(wikidataId);
      note.setUpdatedAt(currentUTCTimestamp);
      return note;
    }
  }

  static class NotesTestData {
    @Setter private List<NoteTestData> noteTestData;
    @Setter private String externalIdentifier;
    @Setter private String circleName; // optional

    private Map<String, Note> buildIndividualNotes(User user, Timestamp currentUTCTimestamp) {
      return noteTestData.stream()
          .map(noteTestData -> noteTestData.buildNote(user, currentUTCTimestamp))
          .collect(Collectors.toMap(note -> note.getTopicConstructor(), n -> n));
    }

    private void buildNoteTree(
        User user,
        Ownership ownership,
        Map<String, Note> titleNoteMap,
        ModelFactoryService modelFactoryService) {
      noteTestData.forEach(
          injection -> {
            Note note = titleNoteMap.get(injection.title);

            if (Strings.isBlank(injection.parentTitle)) {
              note.buildNotebookForHeadNote(ownership, user);
              modelFactoryService.save(note.getNotebook());
            } else {
              note.setParentNote(
                  getParentNote(
                      titleNoteMap, modelFactoryService.noteRepository, injection.parentTitle));
            }
          });
    }

    private Note getParentNote(
        Map<String, Note> titleNoteMap, NoteRepository noteRepository, String parentTitle) {
      Note parentNote = titleNoteMap.get(parentTitle);
      if (parentNote != null) return parentNote;
      return noteRepository.findFirstByTitle(parentTitle);
    }

    private void saveByOriginalOrder(
        Map<String, Note> titleNoteMap, ModelFactoryService modelFactoryService) {
      noteTestData.forEach((inject -> modelFactoryService.save(titleNoteMap.get(inject.title))));
    }
  }

  @PostMapping("/inject_notes")
  @Transactional
  public Map<String, Integer> injectNotes(@RequestBody NotesTestData notesTestData) {
    final User user =
        getUserModelByExternalIdentifierOrCurrentUser(notesTestData.externalIdentifier);
    Ownership ownership = getOwnership(notesTestData, user);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Map<String, Note> titleNoteMap = notesTestData.buildIndividualNotes(user, currentUTCTimestamp);
    notesTestData.buildNoteTree(user, ownership, titleNoteMap, this.modelFactoryService);
    notesTestData.saveByOriginalOrder(titleNoteMap, this.modelFactoryService);
    return titleNoteMap.values().stream()
        .collect(Collectors.toMap(note -> note.getTopicConstructor(), Note::getId));
  }

  @PostMapping("/inject-predefined-questions")
  @Transactional
  public List<PredefinedQuestion> injectPredefinedQuestion(
      @RequestBody PredefinedQuestionsTestData predefinedQuestionsTestData) {
    List<PredefinedQuestion> predefinedQuestions =
        predefinedQuestionsTestData.buildPredefinedQuestions(this.modelFactoryService);
    predefinedQuestions.forEach(question -> modelFactoryService.save(question));
    updateNotebookSettings(
        predefinedQuestions, predefinedQuestionsTestData.getNotebookCertifiable());
    return predefinedQuestions;
  }

  private void updateNotebookSettings(
      List<PredefinedQuestion> predefinedQuestions, Boolean notebookCertifiable) {
    if (predefinedQuestions.isEmpty()) {
      return;
    }
    Notebook notebook = predefinedQuestions.getFirst().getNote().getNotebook();
    notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(predefinedQuestions.size());
    modelFactoryService.save(notebook);
    if (notebookCertifiable != null && notebookCertifiable) {
      modelFactoryService
          .notebookService(notebook)
          .requestNotebookApproval()
          .approve(testabilitySettings.getCurrentUTCTimestamp());
    }
  }

  private Ownership getOwnership(NotesTestData notesTestData, User user) {
    if (notesTestData.circleName != null) {
      Circle circle = modelFactoryService.circleRepository.findByName(notesTestData.circleName);
      return circle.getOwnership();
    }
    return user.getOwnership();
  }

  @PostMapping("/link_notes")
  @Transactional
  public String linkNotes(@RequestBody HashMap<String, String> linkInfo) {
    Note sourceNote =
        modelFactoryService.entityManager.find(
            Note.class, Integer.valueOf(linkInfo.get("source_id")));
    Note targetNote =
        modelFactoryService.entityManager.find(
            Note.class, Integer.valueOf(linkInfo.get("target_id")));
    LinkType type = LinkType.fromLabel(linkInfo.get("type"));
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    User creator = sourceNote.getCreator();
    modelFactoryService.createLink(sourceNote, targetNote, creator, type, currentUTCTimestamp);
    return "OK";
  }

  private User getUserModelByExternalIdentifierOrCurrentUser(String externalIdentifier) {
    if (Strings.isEmpty(externalIdentifier)) {
      User user = currentUser.getEntity();
      if (user == null) {
        throw new RuntimeException("There is no current user");
      }
      return user;
    }
    return getUserModelByExternalIdentifier(externalIdentifier);
  }

  @PostMapping("/share_to_bazaar")
  @Transactional
  public String shareToBazaar(@RequestBody HashMap<String, String> map) {
    Note note = noteRepository.findFirstByTitle(map.get("noteTopology"));
    bazaarService.shareNotebook(note.getNotebook());
    return "OK";
  }

  @PostMapping("/update_current_user")
  @Transactional
  public String updateCurrentUser(@RequestBody HashMap<String, String> userInfo) {
    if (userInfo.containsKey("daily_assimilation_count")) {
      currentUser.setAndSaveDailyAssimilationCount(
          Integer.valueOf(userInfo.get("daily_assimilation_count")));
    }
    if (userInfo.containsKey("space_intervals")) {
      currentUser.setAndSaveSpaceIntervals(userInfo.get("space_intervals"));
    }
    return "OK";
  }

  @PostMapping("/inject_circle")
  @Transactional
  public String injectCircle(@RequestBody HashMap<String, String> circleInfo) {
    Circle entity = new Circle();
    entity.setName(circleInfo.get("circleName"));
    modelFactoryService.save(entity);
    Arrays.stream(circleInfo.get("members").split(","))
        .map(String::trim)
        .forEach(
            s -> {
              circleService.joinAndSave(entity, getUserModelByExternalIdentifier(s));
            });
    return "OK";
  }

  static class SuggestedQuestionsData {
    @Setter private List<QuestionSuggestionParams> examples;
  }

  @PostMapping("/inject_suggested_questions")
  @Transactional
  public String injectSuggestedQuestion(@RequestBody SuggestedQuestionsData testData) {
    testData.examples.forEach(
        example -> {
          SuggestedQuestionForFineTuning suggestion = new SuggestedQuestionForFineTuning();
          suggestion.setUser(currentUser.getEntity());
          suggestedQuestionForFineTuningService.update(suggestion, example);
        });
    return "OK";
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

  @PostMapping(value = "/randomizer")
  public List<Object> randomizer(@RequestBody Randomization randomization) {
    testabilitySettings.setRandomization(randomization);
    return Collections.emptyList();
  }
}

package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.repositories.LinkRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GithubService;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"e2e", "test"})
@RequestMapping("/api/testability")
class TestabilityRestController {

  @Autowired EntityManagerFactory emf;
  @Autowired NoteRepository noteRepository;
  @Autowired LinkRepository linkRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserModel currentUser;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired TestabilitySettings testabilitySettings;

  @PostMapping("/clean_db_and_reset_testability_settings")
  @Transactional
  public String resetDBAndTestabilitySettings() {
    new DBCleanerWorker(emf).truncateAllTables();
    createUser("old_learner", "Old Learner");
    createUser("another_old_learner", "Another Old Learner");
    createUser("admin", "admin");
    createUser("non_admin", "Non Admin");
    testabilitySettings.setUseRealGithub(false);
    testabilitySettings.enableFeatureToggle(false);
    testabilitySettings.setAlwaysChoose("first");
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

  static class SeedNote {
    public String topicConstructor;
    @Setter private String details;
    @Setter private String testingParent;
    @Setter private Boolean skipReview;
    @Setter private String url;
    @Setter private String pictureUrl;
    @Setter private String pictureMask;
    @Setter private String wikidataId;

    private Note buildNote(User user, Timestamp currentUTCTimestamp) {
      Note note = Note.createNote(user, currentUTCTimestamp, new TextContent());
      NoteAccessories content = note.getNoteAccessories();

      note.setTopicConstructor(topicConstructor);
      note.setDetails(details);
      note.setUpdatedAt(currentUTCTimestamp);
      if (skipReview != null) {
        ReviewSetting masterReviewSetting = new ReviewSetting();
        masterReviewSetting.setSkipReview(skipReview);
        note.setMasterReviewSetting(masterReviewSetting);
      }
      content.setUrl(url);
      content.setPictureMask(pictureMask);
      content.setPictureUrl(pictureUrl);

      note.setWikidataId(wikidataId);
      note.setUpdatedAt(currentUTCTimestamp);
      return note;
    }
  }

  static class SeedInfo {
    @Setter private List<SeedNote> seedNotes;
    @Setter private String externalIdentifier;
    @Setter private String circleName; // optional

    private Map<String, Note> buildIndividualNotes(User user, Timestamp currentUTCTimestamp) {
      return seedNotes.stream()
          .map(seedNote -> seedNote.buildNote(user, currentUTCTimestamp))
          .collect(Collectors.toMap(Note::getTopic, n -> n));
    }

    private void buildNoteTree(
        User user,
        Ownership ownership,
        Map<String, Note> titleNoteMap,
        NoteRepository noteRepository) {
      seedNotes.forEach(
          seed -> {
            Note note = titleNoteMap.get(seed.topicConstructor);

            if (Strings.isBlank(seed.testingParent)) {
              note.buildNotebookForHeadNote(ownership, user);
            } else {
              note.setParentNote(getParentNote(titleNoteMap, noteRepository, seed.testingParent));
            }
          });
    }

    private Note getParentNote(
        Map<String, Note> titleNoteMap, NoteRepository noteRepository, String testingParent) {
      Note parentNote = titleNoteMap.get(testingParent);
      if (parentNote != null) return parentNote;
      return noteRepository.findFirstByTitle(testingParent);
    }

    private void saveByOriginalOrder(
        Map<String, Note> titleNoteMap, ModelFactoryService modelFactoryService) {
      seedNotes.forEach(
          (seed -> modelFactoryService.save(titleNoteMap.get(seed.topicConstructor))));
    }
  }

  @PostMapping("/seed_notes")
  @Transactional
  public Map<String, Integer> seedNote(@RequestBody SeedInfo seedInfo) {
    final User user = getUserModelByExternalIdentifierOrCurrentUser(seedInfo.externalIdentifier);
    Ownership ownership = getOwnership(seedInfo, user);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Map<String, Note> titleNoteMap = seedInfo.buildIndividualNotes(user, currentUTCTimestamp);
    seedInfo.buildNoteTree(user, ownership, titleNoteMap, this.noteRepository);
    seedInfo.saveByOriginalOrder(titleNoteMap, this.modelFactoryService);
    return titleNoteMap.values().stream().collect(Collectors.toMap(Note::getTopic, Thingy::getId));
  }

  private Ownership getOwnership(SeedInfo seedInfo, User user) {
    if (seedInfo.circleName != null) {
      Circle circle = modelFactoryService.circleRepository.findByName(seedInfo.circleName);
      return circle.getOwnership();
    }
    return user.getOwnership();
  }

  @PostMapping("/link_notes")
  @Transactional
  public String linkNotes(@RequestBody HashMap<String, String> linkInfo) {
    Note sourceNote = noteRepository.findById(Integer.valueOf(linkInfo.get("source_id"))).get();
    Note targetNote = noteRepository.findById(Integer.valueOf(linkInfo.get("target_id"))).get();
    LinkType type = LinkType.fromLabel(linkInfo.get("type"));
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    Link link =
        Link.createLink(
            sourceNote, targetNote, sourceNote.getThing().getCreator(), type, currentUTCTimestamp);

    modelFactoryService.save(link);
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
    Note note = noteRepository.findFirstByTitle(map.get("noteTopic"));
    modelFactoryService.toBazaarModel().shareNote(note.getNotebook());
    return "OK";
  }

  @PostMapping("/update_current_user")
  @Transactional
  public String updateCurrentUser(@RequestBody HashMap<String, String> userInfo) {
    if (userInfo.containsKey("daily_new_notes_count")) {
      currentUser.setAndSaveDailyNewNotesCount(
          Integer.valueOf(userInfo.get("daily_new_notes_count")));
    }
    if (userInfo.containsKey("space_intervals")) {
      currentUser.setAndSaveSpaceIntervals(userInfo.get("space_intervals"));
    }
    return "OK";
  }

  @PostMapping("/seed_circle")
  @Transactional
  public String seedCircle(@RequestBody HashMap<String, String> circleInfo) {
    Circle entity = new Circle();
    entity.setName(circleInfo.get("circleName"));
    CircleModel circleModel = modelFactoryService.toCircleModel(entity);
    Arrays.stream(circleInfo.get("members").split(","))
        .map(String::trim)
        .forEach(
            s -> {
              circleModel.joinAndSave(getUserModelByExternalIdentifier(s));
            });
    return "OK";
  }

  static class SeedSuggestedQuestions {
    @Setter private List<QuestionSuggestionParams> examples;
  }

  @PostMapping("/seed_suggested_questions")
  @Transactional
  public String seedSuggestedQuestion(@RequestBody SeedSuggestedQuestions seed) {
    seed.examples.forEach(
        example -> {
          SuggestedQuestionForFineTuning suggestion = new SuggestedQuestionForFineTuning();
          suggestion.setUser(currentUser.getEntity());
          modelFactoryService.toSuggestedQuestionForFineTuningService(suggestion).update(example);
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
  public Map<String, String> replaceServiceUrl(
      @RequestBody Map<String, String> setWikidataService) {
    return testabilitySettings.replaceServiceUrls(setWikidataService);
  }

  static class Randomization {
    public String choose;
  }

  @PostMapping(value = "/randomizer")
  public List<Object> randomizer(@RequestBody Randomization randomization) {
    testabilitySettings.setAlwaysChoose(randomization.choose);
    return Collections.emptyList();
  }
}

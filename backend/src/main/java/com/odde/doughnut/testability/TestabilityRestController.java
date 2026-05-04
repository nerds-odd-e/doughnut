package com.odde.doughnut.testability;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.CircleRepository;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.CircleService;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.NotebookCertificateApprovalService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.services.SuggestedQuestionForFineTuningService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.model.PredefinedQuestionsTestData;
import com.odde.doughnut.utils.TimestampOperations;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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
  @Autowired CircleRepository circleRepository;
  @Autowired EntityPersister entityPersister;
  @Autowired CircleService circleService;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;
  @Autowired BazaarService bazaarService;
  @Autowired UserService userService;
  @Autowired NotebookService notebookService;
  @Autowired NotebookCertificateApprovalService notebookCertificateApprovalService;
  @Autowired NoteService noteService;
  @Autowired FolderRepository folderRepository;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

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

  static class NoteTestData {
    @JsonProperty("Title")
    public String title;

    @JsonProperty("Details")
    @Setter
    private String details;

    @JsonProperty("Skip Memory Tracking")
    @Setter
    private Boolean skipMemoryTracking;

    @JsonProperty("Remember Spelling")
    @Setter
    private Boolean rememberSpelling;

    @JsonProperty("Image Url")
    @Setter
    private String imageUrl;

    @JsonProperty("Image Mask")
    @Setter
    private String imageMask;

    @JsonProperty("Wikidata Id")
    @Setter
    private String wikidataId;

    @Schema(
        name = "Folder",
        description =
            "Notebook-local folder path (segments separated by /). E2E/testability only: missing"
                + " folder rows are created here, then the note is assigned that folder. Production"
                + " note APIs do not accept or infer folder paths.")
    @JsonProperty("Folder")
    @Getter
    @Setter
    private String folder;

    private Note buildNote(User user, Timestamp currentUTCTimestamp) {
      Note note = new Note();
      note.initializeNewNote(user, null, currentUTCTimestamp, title);
      NoteAccessory content = note.getOrInitializeNoteAccessory();

      note.setTitle(title);
      note.setDetails(details);
      note.setUpdatedAt(currentUTCTimestamp);
      if (skipMemoryTracking != null) {
        note.getRecallSetting().setSkipMemoryTracking(skipMemoryTracking);
      }
      if (rememberSpelling != null) {
        note.getRecallSetting().setRememberSpelling(rememberSpelling);
      }
      content.setImageMask(imageMask);
      content.setImageUrl(imageUrl);

      note.setWikidataId(wikidataId);
      note.setUpdatedAt(currentUTCTimestamp);
      return note;
    }
  }

  @Schema(name = "NotesTestData")
  static class NotesTestData {
    @Getter @Setter private List<NoteTestData> noteTestData;
    @Setter private String externalIdentifier;
    @Setter private String circleName; // optional

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Getter
    @Setter
    private String notebookName;

    private Map<String, Note> buildIndividualNotes(User user, Timestamp currentUTCTimestamp) {
      return noteTestData.stream()
          .map(noteTestData -> noteTestData.buildNote(user, currentUTCTimestamp))
          .collect(Collectors.toMap(note -> note.getTitle(), n -> n));
    }

    private void buildNoteTree(
        User user,
        Ownership ownership,
        String notebookName,
        Notebook notebookFromRepositoryOrNull,
        Timestamp currentUTCTimestamp,
        Map<String, Note> titleNoteMap,
        EntityPersister entityPersister) {
      Note firstRootCreatedInBatch = null;
      for (NoteTestData injection : noteTestData) {
        Note note = titleNoteMap.get(injection.title);
        if (!Strings.isBlank(injection.getFolder())) {
          if (notebookFromRepositoryOrNull != null) {
            note.initializeAsNotebookRoot(
                notebookFromRepositoryOrNull, user, currentUTCTimestamp, injection.title);
            notebookFromRepositoryOrNull.setUpdated_at(currentUTCTimestamp);
            entityPersister.merge(notebookFromRepositoryOrNull);
          } else if (firstRootCreatedInBatch == null) {
            note.attachToNewNotebook(ownership, user);
            note.getNotebook().setName(notebookName);
            note.getNotebook().setUpdated_at(currentUTCTimestamp);
            entityPersister.save(note.getNotebook());
            firstRootCreatedInBatch = note;
          } else {
            note.initializeAsNotebookRoot(
                firstRootCreatedInBatch.getNotebook(), user, currentUTCTimestamp, injection.title);
          }
          continue;
        }
        if (notebookFromRepositoryOrNull != null) {
          note.initializeAsNotebookRoot(
              notebookFromRepositoryOrNull, user, currentUTCTimestamp, injection.title);
          notebookFromRepositoryOrNull.setUpdated_at(currentUTCTimestamp);
          entityPersister.merge(notebookFromRepositoryOrNull);
          continue;
        }
        if (firstRootCreatedInBatch == null) {
          note.attachToNewNotebook(ownership, user);
          note.getNotebook().setName(notebookName);
          note.getNotebook().setUpdated_at(currentUTCTimestamp);
          entityPersister.save(note.getNotebook());
          firstRootCreatedInBatch = note;
        } else {
          note.initializeAsNotebookRoot(
              firstRootCreatedInBatch.getNotebook(), user, currentUTCTimestamp, injection.title);
        }
      }
    }

    private void saveByOriginalOrder(
        Map<String, Note> titleNoteMap, EntityPersister entityPersister) {
      noteTestData.forEach(inject -> entityPersister.save(titleNoteMap.get(inject.title)));
    }
  }

  private void applyExplicitFolderPlacements(
      List<NoteTestData> injections, Map<String, Note> titleNoteMap, Timestamp now) {
    for (NoteTestData injection : injections) {
      if (Strings.isBlank(injection.getFolder())) {
        continue;
      }
      Note note = titleNoteMap.get(injection.title);
      Folder folder = resolveOrCreateFolderPath(note.getNotebook(), injection.getFolder(), now);
      note.setFolder(folder);
    }
  }

  private Folder resolveOrCreateFolderPath(Notebook notebook, String folderPath, Timestamp now) {
    Folder parent = null;
    for (String rawSegment : folderPath.split("/")) {
      String name = rawSegment.trim();
      if (name.isEmpty()) {
        continue;
      }
      Integer parentFolderId = parent == null ? null : parent.getId();
      List<Folder> candidates =
          folderRepository.findCandidateChildContainers(notebook.getId(), parentFolderId, name);
      if (!candidates.isEmpty()) {
        parent = candidates.getFirst();
        continue;
      }
      Folder created = new Folder();
      created.setNotebook(notebook);
      created.setParentFolder(parent);
      created.setName(name);
      created.setCreatedAt(now);
      created.setUpdatedAt(now);
      entityPersister.save(created);
      parent = created;
    }
    if (parent == null) {
      throw new RuntimeException("Folder path resolved to no folder: `" + folderPath + "`");
    }
    return parent;
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
    if (Strings.isEmpty(notesTestData.externalIdentifier)) {
      throw new RuntimeException("externalIdentifier is required and cannot be empty");
    }
    if (Strings.isEmpty(notesTestData.notebookName)) {
      throw new RuntimeException("notebookName is required and cannot be empty");
    }
    final User user = getUserModelByExternalIdentifier(notesTestData.externalIdentifier);
    Ownership ownership = getOwnership(notesTestData, user);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    List<NoteTestData> injections =
        Optional.ofNullable(notesTestData.getNoteTestData()).orElseGet(Collections::emptyList);
    notesTestData.setNoteTestData(injections);
    if (injections.isEmpty()) {
      Optional<Notebook> existingNotebook =
          notebookRepository.findFirstByNameAndDeletedAtIsNullOrderByIdAsc(
              notesTestData.notebookName);
      if (existingNotebook.isPresent()) {
        Notebook nb = existingNotebook.get();
        if (!Objects.equals(nb.getOwnership().getId(), ownership.getId())) {
          throw new RuntimeException(
              "Notebook named `"
                  + notesTestData.notebookName
                  + "` exists but belongs to different ownership.");
        }
        return Collections.emptyMap();
      }
      notebookService.createNotebookForOwnership(
          ownership, user, currentUTCTimestamp, notesTestData.notebookName, null);
      return Collections.emptyMap();
    }

    Notebook notebookFromRepository =
        notebookRepository
            .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(notesTestData.notebookName)
            .map(
                nb -> {
                  if (!Objects.equals(nb.getOwnership().getId(), ownership.getId())) {
                    throw new RuntimeException(
                        "Notebook named `"
                            + notesTestData.notebookName
                            + "` exists but belongs to different ownership.");
                  }
                  return nb;
                })
            .orElse(null);
    Map<String, Note> titleNoteMap = notesTestData.buildIndividualNotes(user, currentUTCTimestamp);
    notesTestData.buildNoteTree(
        user,
        ownership,
        notesTestData.notebookName,
        notebookFromRepository,
        currentUTCTimestamp,
        titleNoteMap,
        this.entityPersister);
    applyExplicitFolderPlacements(injections, titleNoteMap, currentUTCTimestamp);
    notesTestData.saveByOriginalOrder(titleNoteMap, this.entityPersister);
    for (Note note : titleNoteMap.values()) {
      wikiTitleCacheService.refreshForNote(note, user);
    }
    return titleNoteMap.values().stream()
        .collect(Collectors.toMap(note -> note.getTitle(), Note::getId));
  }

  @PostMapping("/inject-predefined-questions")
  @Transactional
  public List<PredefinedQuestion> injectPredefinedQuestion(
      @RequestBody PredefinedQuestionsTestData predefinedQuestionsTestData) {
    List<PredefinedQuestion> predefinedQuestions =
        predefinedQuestionsTestData.buildPredefinedQuestions(this.noteRepository);
    predefinedQuestions.forEach(question -> entityPersister.save(question));
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
    entityPersister.save(notebook);
    if (notebookCertifiable != null && notebookCertifiable) {
      NotebookCertificateApproval approval = notebookService.requestNotebookApproval(notebook);
      notebookCertificateApprovalService.approve(
          approval, testabilitySettings.getCurrentUTCTimestamp());
    }
  }

  private Ownership getOwnership(NotesTestData notesTestData, User user) {
    if (notesTestData.circleName != null) {
      Circle circle = circleRepository.findByName(notesTestData.circleName);
      return circle.getOwnership();
    }
    return user.getOwnership();
  }

  @PostMapping("/create_relationships")
  @Transactional
  public String createRelationships(@RequestBody HashMap<String, String> relationshipInfo) {
    Note sourceNote =
        entityPersister.find(Note.class, Integer.valueOf(relationshipInfo.get("source_id")));
    Note targetNote =
        entityPersister.find(Note.class, Integer.valueOf(relationshipInfo.get("target_id")));
    RelationType type = RelationType.fromLabel(relationshipInfo.get("type"));
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    User creator = sourceNote.getCreator();
    noteService.createRelationship(
        sourceNote,
        targetNote,
        creator,
        type,
        currentUTCTimestamp,
        RelationshipNotePlacement.RELATIONS_SUBFOLDER);
    return "OK";
  }

  @PostMapping("/share_to_bazaar")
  @Transactional
  public String shareToBazaar(@RequestBody ShareToBazaarRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    Notebook notebook =
        notebookRepository
            .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(request.getNotebookName())
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
    if (userInfo.containsKey("space_intervals")) {
      userService.setSpaceIntervals(user, userInfo.get("space_intervals"));
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
    return "OK";
  }

  static class SuggestedQuestionsData {
    @Setter private List<QuestionSuggestionParams> examples;
    @Setter private String username;
  }

  @PostMapping("/inject_suggested_questions")
  @Transactional
  public String injectSuggestedQuestion(@RequestBody SuggestedQuestionsData testData) {
    if (Strings.isEmpty(testData.username)) {
      throw new RuntimeException("username is required and cannot be empty");
    }
    User user = getUserModelByExternalIdentifier(testData.username);
    testData.examples.forEach(
        example -> {
          SuggestedQuestionForFineTuning suggestion = new SuggestedQuestionForFineTuning();
          suggestion.setUser(user);
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

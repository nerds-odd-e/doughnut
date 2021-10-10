
package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.LinkRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GithubService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@Profile({ "e2e", "test", "dev" })
@RequestMapping("/api/testability")
class TestabilityRestController {

    @Autowired
    EntityManagerFactory emf;
    @Autowired
    NoteRepository noteRepository;
    @Autowired
    LinkRepository linkRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CurrentUserFetcherFromRequest currentUser;

    @Autowired
    ModelFactoryService modelFactoryService;
    @Autowired
    TestabilitySettings testabilitySettings;

    @PostMapping("/clean_db_and_reset_testability_settings")
    public String resetDBAndTestabilitySettings() {
        new DBCleanerWorker(emf).truncateAllTables();
        createUser("old_learner", "Old Learner");
        createUser("another_old_learner", "Another Old Learner");
        createUser("developer", "Developer");
        createUser("non_developer", "Non Developer");
        testabilitySettings.setUseRealGithub(false);
        testabilitySettings.enableFeatureToggle(false);
        return "OK";
    }

    @PostMapping("/feature_toggle")
    public String enableFeatureToggle(@RequestBody Map<String, String> requestBody) {
        testabilitySettings.enableFeatureToggle(requestBody.get("enabled").equals("true"));
        return "OK";
    }

    @GetMapping("/feature_toggle")
    public Boolean getFeatureToggle() {
        return testabilitySettings.isFeatureToggleEnabled();
    }

    private void createUser(String externalIdentifier, String name) {
        User user = new User();
        user.setExternalIdentifier(externalIdentifier);
        user.setName(name);
        userRepository.save(user);
    }

    @PostMapping("/seed_notes")
    public List<Integer> seedNote(@RequestBody List<NoteContent> noteContents, @RequestParam(name = "external_identifier") String externalIdentifier) throws Exception {
        final User user = getUserModelByExternalIdentifierOrCurrentUser(externalIdentifier).getEntity();
        HashMap<String, Note> earlyNotes = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        for (NoteContent content : noteContents) {
            Note note = new Note();
            note.mergeNoteContent(content);
            note.setCreatedAtAndUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
            earlyNotes.put(content.getTitle(), note);
            noteList.add(note);
            final String testingParent = note.getNoteContent().getTestingParent();
            if (Strings.isBlank(testingParent)) {
                note.buildNotebookForHeadNote(user.getOwnership(), user);
            }
            else {
                note.setParentNote(earlyNotes.get(testingParent));
            }
            note.setUser(user);
        }

        noteRepository.saveAll(noteList);

        return noteList.stream().map(Note::getId).collect(Collectors.toList());
    }

    @PostMapping("/link_notes")
    @Transactional
    public String linkNotes(@RequestBody HashMap<String, String> linkInfo) {
        Link link = new Link();
        link.setTargetNote(noteRepository.findById(Integer.valueOf(linkInfo.get("target_id"))).get());
        Note sourceNote = noteRepository.findById(Integer.valueOf(linkInfo.get("source_id"))).get();
        link.setSourceNote(sourceNote);
        link.setUser(sourceNote.getUser());
        link.setLinkType(Link.LinkType.fromLabel(linkInfo.get("type")));
        linkRepository.save(link);
        return "OK";
    }

    private UserModel getUserModelByExternalIdentifierOrCurrentUser(String externalIdentifier) {
        if (Strings.isEmpty(externalIdentifier)) {
            if (currentUser.getUser() == null) {
                throw new RuntimeException("There is no current user");
            }
            return currentUser.getUser();
        }
        return getUserModelByExternalIdentifier(externalIdentifier);
    }

    @PostMapping("/share_to_bazaar")
    public String shareToBazaar(@RequestBody HashMap<String, String> map) {
        Note note = noteRepository.findFirstByTitle(map.get("noteTitle"));
        modelFactoryService.toBazaarModel().shareNote(note.getNotebook());
        return "OK";
    }

    @PostMapping("/update_current_user")
    @Transactional
    public String updateCurrentUser(@RequestBody HashMap<String, String> userInfo) {
        UserModel currentUserModel = currentUser.getUser();
        if (userInfo.containsKey("daily_new_notes_count")) {
            currentUserModel.setAndSaveDailyNewNotesCount(Integer.valueOf(userInfo.get("daily_new_notes_count")));
        }
        if (userInfo.containsKey("space_intervals")) {
            currentUserModel.setAndSaveSpaceIntervals(userInfo.get("space_intervals"));
        }
        return "OK";
    }

    @PostMapping("/seed_circle")
    public String seedCircle(@RequestBody HashMap<String, String> circleInfo) {
        Circle entity = new Circle();
        entity.setName(circleInfo.get("circleName"));
        CircleModel circleModel = modelFactoryService.toCircleModel(entity);
        Arrays.stream(circleInfo.get("members").split(",")).map(String::trim).forEach(s->{
            circleModel.joinAndSave(getUserModelByExternalIdentifier(s));
        });
        return "OK";
    }

    private UserModel getUserModelByExternalIdentifier(String externalIdentifier) {
        User user = userRepository.findByExternalIdentifier(externalIdentifier);
        if (user != null) {
            return modelFactoryService.toUserModel(user);
        }
        throw new RuntimeException("User with external identifier `" + externalIdentifier + "` does not exist");
    }

    static DateTimeFormatter getDateTimeFormatter() {
        String pattern = "\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter;
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

    @PostMapping(value="/time_travel")
    public List<Object> timeTravel(@RequestBody  TimeTravel timeTravel) {
        DateTimeFormatter formatter = TestabilityRestController.getDateTimeFormatter();
        LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(timeTravel.travel_to));
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        testabilitySettings.timeTravelTo(timestamp);
        return Collections.emptyList();
    }

    static class TimeTravelRelativeToNow {
        public Integer hours;
    }

    @PostMapping(value="/time_travel_relative_to_now")
    public List<Object> timeTravelRelativeToNow(@RequestBody  TimeTravelRelativeToNow timeTravelRelativeToNow) {
        DateTimeFormatter formatter = TestabilityRestController.getDateTimeFormatter();
        Timestamp timestamp = TimestampOperations.addHoursToTimestamp(new Timestamp(System.currentTimeMillis()), timeTravelRelativeToNow.hours);
        testabilitySettings.timeTravelTo(timestamp);
        return Collections.emptyList();
    }

    static class Randomization {
        public String choose;
    }

    @PostMapping(value="/randomizer")
    public List<Object> randomizer(@RequestBody  Randomization randomization) {
        testabilitySettings.setAlwaysChoose(randomization.choose);
        return Collections.emptyList();
    }

}


package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.NoteContentModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@Profile({"test", "dev"})
@RequestMapping("/api/testability")
class TestabilityController {
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    NoteRepository noteRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CurrentUserFetcherFromRequest currentUser;
    @Autowired
    ModelFactoryService modelFactoryService;
    @Autowired
    TimeTraveler timeTraveler;

    @GetMapping("/clean_db_and_seed_data")
    public String cleanDBAndSeedData() {
        new DBCleanerWorker(emf).truncateAllTables();
        UserEntity userEntity = new UserEntity();
        userEntity.setExternalIdentifier("old_learner");
        userEntity.setName("Old Learner");
        userRepository.save(userEntity);
        return "OK";
    }

    @PostMapping("/seed_notes")
    public List<Integer> seedNote(@RequestBody List<NoteEntity> notes, @RequestParam(name = "external_identifier") String externalIdentifier) throws Exception {
        UserModel userModel = getUserModelByExternalIdentifierOrCurrentUser(externalIdentifier);
        HashMap<String, NoteEntity> earlyNotes = new HashMap<>();

        for (NoteEntity note : notes) {
            earlyNotes.put(note.getTitle(), note);
            NoteContentModel noteModel = modelFactoryService.toNoteModel(note);
            noteModel.setOwnership(userModel);
            note.setParentNote(earlyNotes.get(note.getTestingParent()));
        }
        noteRepository.saveAll(notes);
        return notes.stream().map(NoteEntity::getId).collect(Collectors.toList());
    }

    private UserModel getUserModelByExternalIdentifierOrCurrentUser(String externalIdentifier) throws Exception {
        if (Strings.isEmpty(externalIdentifier)) {
            if (currentUser.getUser() == null) {
                throw new Exception("There is no current user");
            }
            return currentUser.getUser();
        }
        UserEntity userEntity = userRepository.findByExternalIdentifier(externalIdentifier);
        if (userEntity != null) {
            return modelFactoryService.toUserModel(userEntity);
        }
        throw new Exception("User with external identifier `" + externalIdentifier + "` does not exist");
    }

    @PostMapping("/share_to_bazaar")
    public String shareToBazaar(@RequestBody HashMap<String, String> map) throws Exception {
        NoteEntity noteEntity = noteRepository.findFirstByTitle(map.get("noteTitle"));
        modelFactoryService.toBazaarModel().shareNote(noteEntity);
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

    @PostMapping("/time_travel")
    public String timeTravel(@RequestBody HashMap<String, String> userInfo) {
        String pattern = "\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"";
        String travelTo = userInfo.get("travel_to");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(travelTo));
        Timestamp timestamp = Timestamp.valueOf(localDateTime);

        timeTraveler.timeTravelTo(timestamp);
        return "OK";
    }

}

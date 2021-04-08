
package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.LinkRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@Profile({"test", "dev"})
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
    TimeTraveler timeTraveler;

    @PostMapping("/clean_db_and_seed_data")
    public String cleanDBAndSeedData() {
        new DBCleanerWorker(emf).truncateAllTables();
        createUser("old_learner", "Old Learner");
        createUser("another_old_learner", "Another Old Learner");
        return "OK";
    }

    private void createUser(String externalIdentifier, String name) {
        UserEntity userEntity = new UserEntity();
        userEntity.setExternalIdentifier(externalIdentifier);
        userEntity.setName(name);
        userRepository.save(userEntity);
    }

    @PostMapping("/seed_notes")
    public List<Integer> seedNote(@RequestBody List<NoteContentEntity> noteContents, @RequestParam(name = "external_identifier") String externalIdentifier) throws Exception {
        final UserEntity userEntity = getUserModelByExternalIdentifierOrCurrentUser(externalIdentifier).getEntity();
        HashMap<String, NoteEntity> earlyNotes = new HashMap<>();
        List<NoteEntity> noteList = new ArrayList<>();

        for (NoteContentEntity content : noteContents) {
            NoteEntity note = new NoteEntity();
            note.mergeNoteContent(content);
            earlyNotes.put(content.getTitle(), note);
            noteList.add(note);
            final String testingParent = note.getNoteContent().getTestingParent();
            if (Strings.isBlank(testingParent)) {
                note.buildNotebookEntityForHeadNote(userEntity.getOwnershipEntity(), userEntity);
            }
            else {
                note.setParentNote(earlyNotes.get(testingParent));
            }
            note.setUserEntity(userEntity);
        }

        noteRepository.saveAll(noteList);

        return noteList.stream().map(NoteEntity::getId).collect(Collectors.toList());
    }

    @PostMapping("/link_notes")
    @Transactional
    public String linkNotes(@RequestBody HashMap<String, String> userInfo) {
        LinkEntity linkEntity = new LinkEntity();
        linkEntity.setTargetNote(noteRepository.findById(Integer.valueOf(userInfo.get("target_id"))).get());
        NoteEntity sourceNote = noteRepository.findById(Integer.valueOf(userInfo.get("source_id"))).get();
        linkEntity.setSourceNote(sourceNote);
        linkEntity.setUserEntity(sourceNote.getUserEntity());
        linkEntity.setType(userInfo.get("type"));
        linkRepository.save(linkEntity);
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
        NoteEntity noteEntity = noteRepository.findFirstByTitle(map.get("noteTitle"));
        modelFactoryService.toBazaarModel().shareNote(noteEntity.getNotebookEntity());
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
        CircleEntity entity = new CircleEntity();
        entity.setName(circleInfo.get("circleName"));
        CircleModel circleModel = modelFactoryService.toCircleModel(entity);
        Arrays.stream(circleInfo.get("members").split(",")).map(String::trim).forEach(s->{
            circleModel.joinAndSave(getUserModelByExternalIdentifier(s));
        });
        return "OK";
    }

    private UserModel getUserModelByExternalIdentifier(String externalIdentifier) {
        UserEntity userEntity = userRepository.findByExternalIdentifier(externalIdentifier);
        if (userEntity != null) {
            return modelFactoryService.toUserModel(userEntity);
        }
        throw new RuntimeException("User with external identifier `" + externalIdentifier + "` does not exist");
    }

    static DateTimeFormatter getDateTimeFormatter() {
        String pattern = "\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter;
    }

}


package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUserFromRequest;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@Profile({"test", "dev"})
@RequestMapping("/api/testability")
class TestabilityController {
  @Autowired EntityManagerFactory emf;
  @Autowired NoteRepository noteRepository;
  @Autowired UserRepository userRepository;
  @Autowired
  CurrentUserFromRequest currentUser;

  @GetMapping("/clean_db_and_seed_data")
  public String cleanDBAndSeedData() {
    new DBCleanerWorker(emf).truncateAllTables();
    User user = new User();
    user.setExternalIdentifier("old_learner");
    user.setName("Old Learner");
    userRepository.save(user);
    return "OK";
  }

  @PostMapping("/seed_notes")
  public List<Integer> seedNote(@RequestBody List<Note> notes) throws Exception {
    User user = currentUser.getUser();
    if (user == null) throw new Exception("User does not exist");
    HashMap<String, Note> earlyNotes = new HashMap<>();

    for (Note note : notes) {
      earlyNotes.put(note.getTitle(), note);
      note.setUser(user);
      note.setParentNote(earlyNotes.get(note.getTestingLinkTo()));
    }
    noteRepository.saveAll(notes);
    return notes.stream().map(Note::getId).collect(Collectors.toList());
  }
}

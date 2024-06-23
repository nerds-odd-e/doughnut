package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserAssistantThread;
import org.springframework.data.repository.CrudRepository;

public interface UserAssistantThreadRepository
    extends CrudRepository<UserAssistantThread, Integer> {

  UserAssistantThread findByUserAndNote(User user, Note note);
}

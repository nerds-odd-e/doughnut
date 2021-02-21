package com.odde.doughnut.testability;

import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.UserPersistedBuilder;

public class MakeMe {
    private UserRepository userRepository;

    public MakeMe(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPersistedBuilder aUser() {
        return new UserPersistedBuilder(userRepository);
    }

    public NoteBuilder aNote() {
        return new NoteBuilder();
    }
}

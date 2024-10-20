package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ConversationMessage;
import org.springframework.data.repository.CrudRepository;

public interface ConversationMessageRepository
    extends CrudRepository<ConversationMessage, Integer> {}

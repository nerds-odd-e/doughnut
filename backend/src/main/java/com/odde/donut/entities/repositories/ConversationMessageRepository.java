package com.odde.donut.entities.repositories;

import com.odde.donut.entities.ConversationMessage;
import org.springframework.data.repository.CrudRepository;

public interface ConversationMessageRepository
    extends CrudRepository<ConversationMessage, Integer> {}

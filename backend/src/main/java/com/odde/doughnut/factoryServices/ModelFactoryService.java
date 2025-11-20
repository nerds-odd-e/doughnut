package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.repositories.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
  @Autowired public NoteRepository noteRepository;
  @Autowired public BazaarNotebookRepository bazaarNotebookRepository;
  @Autowired public MemoryTrackerRepository memoryTrackerRepository;
  @Autowired public CircleRepository circleRepository;
  @Autowired public FailureReportRepository failureReportRepository;
  @Autowired public GlobalSettingRepository globalSettingRepository;
  @Autowired public AssessmentAttemptRepository assessmentAttemptRepository;
  @Autowired public EntityPersister entityPersister;
  @Autowired public EntityManager entityManager;
  @Autowired public NotebookRepository notebookRepository;
  @Autowired public CertificateRepository certificateRepository;
  @Autowired public ConversationRepository conversationRepository;
  @Autowired public ConversationMessageRepository conversationMessageRepository;
  @Autowired public RecallPromptRepository recallPromptRepository;

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

  @Autowired public NotebookAiAssistantRepository notebookAiAssistantRepository;
}

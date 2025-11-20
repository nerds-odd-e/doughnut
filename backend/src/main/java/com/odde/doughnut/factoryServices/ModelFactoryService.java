package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
  @Autowired public NoteRepository noteRepository;
  @Autowired public FailureReportRepository failureReportRepository;
  @Autowired public AssessmentAttemptRepository assessmentAttemptRepository;
  @Autowired public NotebookRepository notebookRepository;
  @Autowired public CertificateRepository certificateRepository;
  @Autowired public ConversationRepository conversationRepository;
  @Autowired public ConversationMessageRepository conversationMessageRepository;

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;
}

package com.odde.donut.testability;

import com.odde.donut.controllers.currentUser.CurrentUser;
import com.odde.donut.controllers.currentUser.ThreadLocalCurrentUser;
import com.odde.donut.entities.Note;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.GithubService;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import com.odde.donut.services.notebookAttachment.InMemoryNotebookAttachmentContent;
import com.odde.donut.services.notebookGit.SqlStatementCallLogDataSourceConfig;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one Spring application context shared by backend tests of the {@code test} profile. Adding
 * annotations or bean overrides on a subclass creates another cached context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(SqlStatementCallLogDataSourceConfig.class)
public abstract class SpringTestBase {
  @Autowired protected MakeMe makeMe;
  @Autowired protected AuthorizationService authorizationService;
  @Autowired protected TestabilitySettings testabilitySettings;

  @TestBean protected CurrentUser currentUser;
  @MockitoBean protected GithubService githubService;
  @MockitoBean protected HttpClientAdapter httpClientAdapter;

  @MockitoBean(name = "officialOpenAiClient")
  protected OpenAIClient officialClient;

  static CurrentUser currentUser() {
    return new ThreadLocalCurrentUser();
  }

  @AfterEach
  void cleanupSharedTestState() {
    testabilitySettings.timeTravelTo(null);
    testabilitySettings.setOpenAiTokenOverride(null);
    testabilitySettings.setUseRealGithub(true);
    ((InMemoryNotebookAttachmentContent) makeMe.notebookAttachmentContent).clear();
  }

  /** See {@link MakeMe#authorReferencingContent(Note, String)}. */
  protected void authorReferencingContent(Note note, String content) {
    makeMe.authorReferencingContent(note, content);
  }
}

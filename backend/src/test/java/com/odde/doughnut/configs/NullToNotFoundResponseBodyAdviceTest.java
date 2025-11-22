package com.odde.doughnut.configs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NullToNotFoundResponseBodyAdviceTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;

  @TestBean protected CurrentUser currentUser;

  static CurrentUser currentUser() {
    return new CurrentUser();
  }

  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    // Mock null chat completion to make askAQuestion return null
    openAIChatCompletionMock.mockNullChatCompletion();
  }

  @Test
  void shouldReturn404WhenAskAQuestionReturnsNull() throws Exception {
    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    makeMe.aNote().under(note).please(); // Add another note to the notebook
    User user = currentUser.getUser();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();

    this.mockMvc
        .perform(get("/api/recall-prompts/{memoryTracker}/question", memoryTracker.getId()))
        .andExpect(status().isNotFound());
  }
}

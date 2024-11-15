package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.openAiApis.OpenAiApiExtended;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import io.reactivex.Single;
import java.io.IOException;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestAiAudioControllerTests {
  @Autowired MakeMe makeMe;
  RestAiAudioController controller;
  @Mock OpenAiApiExtended openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    controller =
        new RestAiAudioController(
            new OtherAiServices(openAiApi),
            makeMe.modelFactoryService,
            new NotebookAssistantForNoteServiceFactory(
                openAiApi, new GlobalSettingsService(makeMe.modelFactoryService)));
    TextFromAudio completionMarkdownFromAudio = new TextFromAudio();
    completionMarkdownFromAudio.setCompletionMarkdownFromAudio("test123");
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(
        completionMarkdownFromAudio, "audio_transcription_to_text");
    mockTranscriptionSrtResponse("test transcription");
  }

  protected void mockTranscriptionSrtResponse(String responseBody) {
    when(openAiApi.createTranscriptionSrt(any(RequestBody.class)))
        .thenReturn(Single.just(ResponseBody.create(responseBody, null)));
  }

  private MockMultipartFile createMockAudioFile(String filename) {
    return new MockMultipartFile(filename, filename, "audio/mp3", "test".getBytes());
  }

  private AudioUploadDTO createAudioUploadDTO(MockMultipartFile file) {
    var dto = new AudioUploadDTO();
    dto.setUploadAudioFile(file);
    return dto;
  }

  @Nested
  class ConvertAudioToText {
    AudioUploadDTO audioUploadDTO;

    @BeforeEach
    void setup() {
      mockTranscriptionSrtResponse("test");
      audioUploadDTO = new AudioUploadDTO();
      audioUploadDTO.setUploadAudioFile(createMockAudioFile("test.mp3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(createMockAudioFile(filename));
      String result =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudio::getCompletionMarkdownFromAudio)
              .orElse("");
      assertEquals("test123", result);
    }

    @Test
    void convertAudioToText() throws IOException {
      String resp =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudio::getCompletionMarkdownFromAudio)
              .orElse("");
      assertThat(resp, equalTo("test123"));
    }

    @Test
    void usingThePreviousTrailingDetails() throws IOException {
      audioUploadDTO.setPreviousNoteDetails("Long long ago");
      controller.audioToText(audioUploadDTO).map(TextFromAudio::getCompletionMarkdownFromAudio);
      ArgumentCaptor<ChatCompletionRequest> argumentCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi, times(1)).createChatCompletion(argumentCaptor.capture());
      ChatCompletionRequest capturedArgument = argumentCaptor.getValue();
      assertThat(
          capturedArgument.getMessages().get(0).getTextContent(), containsString("Long long ago"));
    }
  }

  @Nested
  class ConvertAudioToTextForNote {
    OpenAIAssistantMocker openAIAssistantMocker;
    OpenAIAssistantThreadMocker openAIAssistantThreadMocker;
    AudioUploadDTO audioUploadDTO;
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      audioUploadDTO = createAudioUploadDTO(createMockAudioFile("test.mp3"));
      openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
      openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation("existing-thread");
      NoteDetailsCompletion completion = new NoteDetailsCompletion();
      completion.completion = "text from audio transcription";

      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(completion, AiToolName.COMPLETE_NOTE_DETAILS.getValue())
          .mockRetrieveRun()
          .mockSubmitOutput();
    }

    @Test
    void convertAudioToTextForExistingNote() throws IOException {
      TextFromAudio result = controller.audioToTextForNote(note, audioUploadDTO);

      verify(openAiApi).createTranscriptionSrt(any(RequestBody.class));
      verify(openAiApi)
          .createThread(
              argThat(
                  request -> {
                    assertThat(
                        request.getMessages().get(0).getContent().toString(),
                        equalTo(note.getNoteDescription()));
                    assertThat(
                        request.getMessages().get(1).getContent().toString(),
                        containsString("test transcription"));
                    return true;
                  }));
      assertNotNull(result);
      assertThat(result.getCompletionMarkdownFromAudio(), equalTo("text from audio transcription"));
    }

    @Test
    void shouldOnlyIncludeCompleteNoteDetailsToolInRun() throws IOException {
      controller.audioToTextForNote(note, audioUploadDTO);

      verify(openAiApi)
          .createRun(
              any(),
              argThat(
                  request -> {
                    assertThat(request.getTools(), hasSize(1));
                    assertThat(request.getTools().getFirst().getType(), equalTo("function"));
                    return true;
                  }));
    }

    @Test
    void shouldSetRawSRTAndRunId() throws IOException {
      TextFromAudio result = controller.audioToTextForNote(note, audioUploadDTO);

      assertNotNull(result.getRawSRT());
      assertNotNull(result.getRunId());
      assertEquals("test transcription", result.getRawSRT());
      assertEquals("my-run-id", result.getRunId());
    }

    @Test
    void shouldUseExistingThreadAndRunWhenProvided() throws IOException {
      audioUploadDTO.setThreadId("existing-thread");
      audioUploadDTO.setRunId("my-run-id");
      audioUploadDTO.setToolCallId("existing-call");

      TextFromAudio result = controller.audioToTextForNote(note, audioUploadDTO);

      assertNotNull(result);
      assertEquals("text from audio transcription", result.getCompletionMarkdownFromAudio());
      assertEquals("my-run-id", result.getRunId());
      verify(openAiApi)
          .submitToolOutputs(
              eq("existing-thread"),
              eq("my-run-id"),
              argThat(
                  arg -> {
                    assertThat(
                        arg.getToolOutputs().getFirst().getToolCallId(), equalTo("existing-call"));
                    assertThat(
                        arg.getToolOutputs().getFirst().getOutput(),
                        containsString("more to process"));
                    assertThat(
                        arg.getToolOutputs().getFirst().getOutput(),
                        containsString("test transcription"));
                    return true;
                  }));
    }
  }
}

package com.odde.doughnut.services.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.focusContext.FocusContextFocusNote;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiNoteAutomationServiceExtractRequestTest {

  private static final String MODEL = "gpt-4";

  private Note note;
  private AiNoteAutomationService service;
  private StructuredResponseCreateParamsSerializer paramsSerializer;

  @BeforeEach
  void setup() {
    note = new Note();
    note.setTitle("title");
    note.setContent("Original content with a key suggestion to extract.");

    GlobalSettingsService globalSettingsService = mock(GlobalSettingsService.class);
    GlobalSettingsService.GlobalSettingsKeyValue evaluationSetting =
        mock(GlobalSettingsService.GlobalSettingsKeyValue.class);
    when(globalSettingsService.globalSettingEvaluation()).thenReturn(evaluationSetting);
    when(evaluationSetting.getValue()).thenReturn(MODEL);

    FocusContextRetrievalService focusContextRetrievalService =
        mock(FocusContextRetrievalService.class);
    when(focusContextRetrievalService.retrieve(any(), any()))
        .thenReturn(
            new FocusContextResult(
                new FocusContextFocusNote(
                    "nb", "title", "", 0, List.of(), List.of(), List.of(), null, "content",
                    false)));

    FocusContextMarkdownRenderer focusContextMarkdownRenderer =
        mock(FocusContextMarkdownRenderer.class);
    when(focusContextMarkdownRenderer.render(any(), any())).thenReturn("Focus Note content only");

    service =
        new AiNoteAutomationService(
            mock(OpenAiApiHandler.class),
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            note);
    paramsSerializer =
        new StructuredResponseCreateParamsSerializer(new ObjectMapperConfig().objectMapper());
  }

  @Test
  void buildExtractNoteRequestBodyReflectsSelectedLayoutItems() {
    NoteRefinementLayout layout = sampleLayout();

    StructuredResponseCreateParams<NoteExtractionResult> params =
        service.buildExtractNoteRequest(layout, List.of("p1-1", "p2"));
    Map<String, Object> body = paramsSerializer.toBodyMap(params);

    assertThat(body).containsKeys("model", "instructions", "input", "text");
    assertThat(body.get("model")).isEqualTo(MODEL);
    assertThat(body.get("max_output_tokens")).isEqualTo(3000);

    @SuppressWarnings("unchecked")
    Map<String, Object> text = (Map<String, Object>) body.get("text");
    assertThat(text).containsKey("format");
    @SuppressWarnings("unchecked")
    Map<String, Object> format = (Map<String, Object>) text.get("format");
    assertThat(format.get("type")).isEqualTo("json_schema");
    assertThat(format).containsKey("schema");

    String instructions = body.get("instructions").toString();
    assertThat(instructions).contains("Full note layout:");
    assertThat(instructions).contains("\"id\" : \"p1-1\"");
    assertThat(instructions).contains("Selected layout item ids to extract together");
    assertThat(instructions).contains("[p1-1, p2]");
    assertThat(instructions).contains("- p1-1: \"key suggestion to extract\"");
    assertThat(instructions).contains("- p2: \"Other point\"");
  }

  private static NoteRefinementLayout sampleLayout() {
    return new NoteRefinementLayout(
        List.of(
            new NoteRefinementLayoutItem(
                "p1",
                "Main concept",
                false,
                List.of(
                    new NoteRefinementLayoutItem(
                        "p1-1", "key suggestion to extract", false, List.of()))),
            new NoteRefinementLayoutItem("p2", "Other point", false, List.of())));
  }
}

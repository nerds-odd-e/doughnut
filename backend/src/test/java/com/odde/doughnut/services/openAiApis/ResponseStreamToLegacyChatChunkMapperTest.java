package com.odde.doughnut.services.openAiApis;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.openai.models.responses.ResponseStreamEvent;
import com.openai.models.responses.ResponseTextDeltaEvent;
import com.openai.models.responses.ResponseTextDoneEvent;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseStreamToLegacyChatChunkMapperTest {

  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Test
  void mapsTextDeltaAndDoneToLegacyChunks() throws Exception {
    ResponseStreamToLegacyChatChunkMapper mapper =
        new ResponseStreamToLegacyChatChunkMapper(objectMapper);

    List<String> deltas =
        mapper.map(
            ResponseStreamEvent.ofOutputTextDelta(
                ResponseTextDeltaEvent.builder()
                    .delta("Hi")
                    .itemId("i1")
                    .outputIndex(0L)
                    .contentIndex(0L)
                    .sequenceNumber(1L)
                    .logprobs(List.of())
                    .build()));
    assertEquals(1, deltas.size());
    JsonNode chunk = objectMapper.readTree(deltas.getFirst());
    assertEquals("Hi", chunk.get("choices").get(0).get("delta").get("content").asText());

    List<String> done =
        mapper.map(
            ResponseStreamEvent.ofOutputTextDone(
                ResponseTextDoneEvent.builder()
                    .text("Hi")
                    .itemId("i1")
                    .outputIndex(0L)
                    .contentIndex(0L)
                    .sequenceNumber(2L)
                    .logprobs(List.of())
                    .build()));
    assertEquals(1, done.size());
    JsonNode doneChunk = objectMapper.readTree(done.getFirst());
    assertEquals("stop", doneChunk.get("choices").get(0).get("finish_reason").asText());
  }
}

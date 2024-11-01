package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
public class ChatAboutNoteService {
  private final String threadId;
  private final AssistantService assistantService;
  private final ModelFactoryService modelFactoryService;

  public List<Message> getMessageList() {
    return assistantService.loadPreviousMessages(threadId);
  }

  public SseEmitter getAIReplySSE() {
    Flowable<AssistantSSE> runStream = assistantService.getRunStream(threadId);
    SseEmitter emitter = new SseEmitter();
    runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);
            if (Objects.equals(sse.getEvent().eventName, "done")) {
              emitter.complete();
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  public void createUserMessage(String userMessage) {
    assistantService.createUserMessage(userMessage, threadId);
  }
}

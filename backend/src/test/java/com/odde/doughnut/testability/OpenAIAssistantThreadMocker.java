package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.Text;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantThreadMocker(OpenAiApi openAiApi, String threadId) {
  public OpenAIAssistantThreadMocker mockCreateMessage() {
    when(openAiApi.createMessage(eq(threadId), ArgumentMatchers.any()))
        .thenReturn(Single.just(new Message()));
    return this;
  }

  public OpenAIAssistantThreadMocker mockCreateRunInProcess(String runId) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus("processing");
    run.setThreadId(threadId);
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .createRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    return this;
  }

  public OpenAIAssistantThreadMocker mockRetrieveRunAndGetCompleted(String runId) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus("completed");
    run.setThreadId(threadId);
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
    return this;
  }

  public void mockListMessages(String msg) {
    Text txt = new Text(msg, List.of());
    MessageContent cnt = new MessageContent();
    cnt.setText(txt);
    List<MessageContent> contentList = List.of(cnt);
    OpenAiResponse<Message> msgs = new OpenAiResponse<>();
    msgs.setData(List.of(Message.builder().content(contentList).build()));
    Mockito.doReturn(Single.just(msgs)).when(openAiApi).listMessages(threadId, Map.of());
  }
}

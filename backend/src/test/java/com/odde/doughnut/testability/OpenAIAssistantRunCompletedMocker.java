package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.Text;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;

public record OpenAIAssistantRunCompletedMocker(OpenAiApi openAiApi, String threadId, Run run) {
  public void mockListMessages(String msg) {
    Text txt = new Text(msg, List.of());
    MessageContent cnt = new MessageContent();
    cnt.setText(txt);
    List<MessageContent> contentList = List.of(cnt);
    OpenAiResponse<Message> msgs = new OpenAiResponse<>();
    msgs.setData(List.of(Message.builder().content(contentList).build()));
    Mockito.doReturn(Single.just(msgs))
        .when(openAiApi)
        .listMessages(threadId, Map.of("order", "asc", "run_id", run.getId()));
  }

  public OpenAIAssistantRunCompletedMocker mockRetrieveRun() {
    Mockito.doReturn(Single.just(run)).when(openAiApi).retrieveRun(eq(threadId), any());
    return this;
  }

  public void mockSubmitOutput() {
    Mockito.doReturn(Single.just(run))
        .when(openAiApi)
        .submitToolOutputs(eq(threadId), any(), any());
  }
}

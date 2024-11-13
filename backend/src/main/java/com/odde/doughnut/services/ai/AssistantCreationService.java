package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssistantCreationService {
  protected final OpenAiApiHandler openAiApiHandler;
  protected final List<AiTool> tools;

  public AssistantCreationService(OpenAiApiHandler openAiApiHandler, List<AiTool> tools) {
    this.openAiApiHandler = openAiApiHandler;
    this.tools = tools;
  }

  public Assistant createDefaultAssistant(String modelName, String assistantName) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tools.stream().map(AiTool::getTool).toList())
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  public Assistant createAssistantWithFile(
      String modelName, String assistantName, String textContent, String additionalInstruction)
      throws IOException {
    ToolResources tooResources = uploadToolResources(assistantName, textContent);
    List<Tool> toolList = new ArrayList<>(tools.stream().map(AiTool::getTool).toList());
    toolList.add(new FileSearchTool());
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .toolResources(tooResources)
            .instructions(
                OpenAIChatRequestBuilder.systemInstruction + "\n\n" + additionalInstruction)
            .tools(toolList)
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private ToolResources uploadToolResources(String assistantName, String textContent)
      throws IOException {
    String fileId =
        openAiApiHandler.uploadTextFile(
            assistantName + ".json", textContent, "assistants", ".json");

    String vectorStoreId = openAiApiHandler.createVectorFile(assistantName, fileId);
    FileSearchResources fileSearchResources = new FileSearchResources();
    fileSearchResources.setVectorStoreIds(List.of(vectorStoreId));
    return new ToolResources(null, fileSearchResources);
  }
}

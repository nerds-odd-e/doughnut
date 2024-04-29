package com.odde.doughnut.services.ai.client;

import com.theokanning.openai.DeleteResult;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantFile;
import com.theokanning.openai.assistants.AssistantFileRequest;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.assistants.ModifyAssistantRequest;
import com.theokanning.openai.audio.CreateSpeechRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.audio.TranslationResult;
import com.theokanning.openai.billing.BillingUsage;
import com.theokanning.openai.billing.Subscription;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.edit.EditRequest;
import com.theokanning.openai.edit.EditResult;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.engine.Engine;
import com.theokanning.openai.file.File;
import com.theokanning.openai.fine_tuning.FineTuningEvent;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.finetune.FineTuneEvent;
import com.theokanning.openai.finetune.FineTuneRequest;
import com.theokanning.openai.finetune.FineTuneResult;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.messages.Message;
import com.theokanning.openai.messages.MessageFile;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.messages.ModifyMessageRequest;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.runs.CreateThreadAndRunRequest;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.runs.RunCreateRequest;
import com.theokanning.openai.runs.RunStep;
import com.theokanning.openai.runs.SubmitToolOutputsRequest;
import com.theokanning.openai.threads.Thread;
import com.theokanning.openai.threads.ThreadRequest;
import io.reactivex.Single;
import java.time.LocalDate;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;

public interface OpenAiApi2 {
  @GET("v1/models")
  Single<OpenAiResponse<Model>> listModels();

  @GET("/v1/models/{model_id}")
  Single<Model> getModel(@Path("model_id") String var1);

  @POST("/v1/completions")
  Single<CompletionResult> createCompletion(@Body CompletionRequest var1);

  @Streaming
  @POST("/v1/completions")
  Call<ResponseBody> createCompletionStream(@Body CompletionRequest var1);

  @POST("/v1/chat/completions")
  Single<ChatCompletionResult> createChatCompletion(@Body ChatCompletionRequest var1);

  @Streaming
  @POST("/v1/chat/completions")
  Call<ResponseBody> createChatCompletionStream(@Body ChatCompletionRequest var1);

  /** @deprecated */
  @Deprecated
  @POST("/v1/engines/{engine_id}/completions")
  Single<CompletionResult> createCompletion(@Path("engine_id") String var1, @Body CompletionRequest var2);

  @POST("/v1/edits")
  Single<EditResult> createEdit(@Body EditRequest var1);

  /** @deprecated */
  @Deprecated
  @POST("/v1/engines/{engine_id}/edits")
  Single<EditResult> createEdit(@Path("engine_id") String var1, @Body EditRequest var2);

  @POST("/v1/embeddings")
  Single<EmbeddingResult> createEmbeddings(@Body EmbeddingRequest var1);

  /** @deprecated */
  @Deprecated
  @POST("/v1/engines/{engine_id}/embeddings")
  Single<EmbeddingResult> createEmbeddings(@Path("engine_id") String var1, @Body EmbeddingRequest var2);

  @GET("/v1/files")
  Single<OpenAiResponse<File>> listFiles();

  @Multipart
  @POST("/v1/files")
  Single<File> uploadFile(@Part("purpose") RequestBody var1, @Part MultipartBody.Part var2);

  @DELETE("/v1/files/{file_id}")
  Single<DeleteResult> deleteFile(@Path("file_id") String var1);

  @GET("/v1/files/{file_id}")
  Single<File> retrieveFile(@Path("file_id") String var1);

  @Streaming
  @GET("/v1/files/{file_id}/content")
  Single<ResponseBody> retrieveFileContent(@Path("file_id") String var1);

  @POST("/v1/fine_tuning/jobs")
  Single<FineTuningJob> createFineTuningJob(@Body FineTuningJobRequest var1);

  @GET("/v1/fine_tuning/jobs")
  Single<OpenAiResponse<FineTuningJob>> listFineTuningJobs();

  @GET("/v1/fine_tuning/jobs/{fine_tuning_job_id}")
  Single<FineTuningJob> retrieveFineTuningJob(@Path("fine_tuning_job_id") String var1);

  @POST("/v1/fine_tuning/jobs/{fine_tuning_job_id}/cancel")
  Single<FineTuningJob> cancelFineTuningJob(@Path("fine_tuning_job_id") String var1);

  @GET("/v1/fine_tuning/jobs/{fine_tuning_job_id}/events")
  Single<OpenAiResponse<FineTuningEvent>> listFineTuningJobEvents(@Path("fine_tuning_job_id") String var1);

  /** @deprecated */
  @Deprecated
  @POST("/v1/fine-tunes")
  Single<FineTuneResult> createFineTune(@Body FineTuneRequest var1);

  @POST("/v1/completions")
  Single<CompletionResult> createFineTuneCompletion(@Body CompletionRequest var1);

  /** @deprecated */
  @Deprecated
  @GET("/v1/fine-tunes")
  Single<OpenAiResponse<FineTuneResult>> listFineTunes();

  /** @deprecated */
  @Deprecated
  @GET("/v1/fine-tunes/{fine_tune_id}")
  Single<FineTuneResult> retrieveFineTune(@Path("fine_tune_id") String var1);

  /** @deprecated */
  @Deprecated
  @POST("/v1/fine-tunes/{fine_tune_id}/cancel")
  Single<FineTuneResult> cancelFineTune(@Path("fine_tune_id") String var1);

  /** @deprecated */
  @Deprecated
  @GET("/v1/fine-tunes/{fine_tune_id}/events")
  Single<OpenAiResponse<FineTuneEvent>> listFineTuneEvents(@Path("fine_tune_id") String var1);

  @DELETE("/v1/models/{fine_tune_id}")
  Single<DeleteResult> deleteFineTune(@Path("fine_tune_id") String var1);

  @POST("/v1/images/generations")
  Single<ImageResult> createImage(@Body CreateImageRequest var1);

  @POST("/v1/images/edits")
  Single<ImageResult> createImageEdit(@Body RequestBody var1);

  @POST("/v1/images/variations")
  Single<ImageResult> createImageVariation(@Body RequestBody var1);

  @POST("/v1/audio/transcriptions")
  Single<TranscriptionResult> createTranscription(@Body RequestBody var1);

  @POST("/v1/audio/translations")
  Single<TranslationResult> createTranslation(@Body RequestBody var1);

  @POST("/v1/audio/speech")
  Single<ResponseBody> createSpeech(@Body CreateSpeechRequest var1);

  @POST("/v1/moderations")
  Single<ModerationResult> createModeration(@Body ModerationRequest var1);

  /** @deprecated */
  @Deprecated
  @GET("v1/engines")
  Single<OpenAiResponse<Engine>> getEngines();

  /** @deprecated */
  @Deprecated
  @GET("/v1/engines/{engine_id}")
  Single<Engine> getEngine(@Path("engine_id") String var1);

  /** @deprecated */
  @Deprecated
  @GET("v1/dashboard/billing/subscription")
  Single<Subscription> subscription();

  /** @deprecated */
  @Deprecated
  @GET("v1/dashboard/billing/usage")
  Single<BillingUsage> billingUsage(@Query("start_date") LocalDate var1, @Query("end_date") LocalDate var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/assistants")
  Single<Assistant> createAssistant(@Body AssistantRequest var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/assistants/{assistant_id}")
  Single<Assistant> retrieveAssistant(@Path("assistant_id") String var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/assistants/{assistant_id}")
  Single<Assistant> modifyAssistant(@Path("assistant_id") String var1, @Body ModifyAssistantRequest var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @DELETE("/v1/assistants/{assistant_id}")
  Single<DeleteResult> deleteAssistant(@Path("assistant_id") String var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/assistants")
  Single<OpenAiResponse<Assistant>> listAssistants(@QueryMap Map<String, Object> var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/assistants/{assistant_id}/files")
  Single<AssistantFile> createAssistantFile(@Path("assistant_id") String var1, @Body AssistantFileRequest var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/assistants/{assistant_id}/files/{file_id}")
  Single<AssistantFile> retrieveAssistantFile(@Path("assistant_id") String var1, @Path("file_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @DELETE("/v1/assistants/{assistant_id}/files/{file_id}")
  Single<DeleteResult> deleteAssistantFile(@Path("assistant_id") String var1, @Path("file_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/assistants/{assistant_id}/files")
  Single<OpenAiResponse<Assistant>> listAssistantFiles(@Path("assistant_id") String var1, @QueryMap Map<String, Object> var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads")
  Single<Thread> createThread(@Body ThreadRequest var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}")
  Single<Thread> retrieveThread(@Path("thread_id") String var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}")
  Single<Thread> modifyThread(@Path("thread_id") String var1, @Body ThreadRequest var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @DELETE("/v1/threads/{thread_id}")
  Single<DeleteResult> deleteThread(@Path("thread_id") String var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/messages")
  Single<Message> createMessage(@Path("thread_id") String var1, @Body MessageRequest var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages/{message_id}")
  Single<Message> retrieveMessage(@Path("thread_id") String var1, @Path("message_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/messages/{message_id}")
  Single<Message> modifyMessage(@Path("thread_id") String var1, @Path("message_id") String var2, @Body ModifyMessageRequest var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages")
  Single<OpenAiResponse<Message>> listMessages(@Path("thread_id") String var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages")
  Single<OpenAiResponse<Message>> listMessages(@Path("thread_id") String var1, @QueryMap Map<String, Object> var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages/{message_id}/files/{file_id}")
  Single<MessageFile> retrieveMessageFile(@Path("thread_id") String var1, @Path("message_id") String var2, @Path("file_id") String var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages/{message_id}/files")
  Single<OpenAiResponse<MessageFile>> listMessageFiles(@Path("thread_id") String var1, @Path("message_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/messages/{message_id}/files")
  Single<OpenAiResponse<MessageFile>> listMessageFiles(@Path("thread_id") String var1, @Path("message_id") String var2, @QueryMap Map<String, Object> var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/runs")
  Single<Run> createRun(@Path("thread_id") String var1, @Body RunCreateRequest var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/runs/{run_id}")
  Single<Run> retrieveRun(@Path("thread_id") String var1, @Path("run_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/runs/{run_id}")
  Single<Run> modifyRun(@Path("thread_id") String var1, @Path("run_id") String var2, @Body Map<String, String> var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/runs")
  Single<OpenAiResponse<Run>> listRuns(@Path("thread_id") String var1, @QueryMap Map<String, String> var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/runs/{run_id}/submit_tool_outputs")
  Single<Run> submitToolOutputs(@Path("thread_id") String var1, @Path("run_id") String var2, @Body SubmitToolOutputsRequest var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/{thread_id}/runs/{run_id}/cancel")
  Single<Run> cancelRun(@Path("thread_id") String var1, @Path("run_id") String var2);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @POST("/v1/threads/runs")
  Single<Run> createThreadAndRun(@Body CreateThreadAndRunRequest var1);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/runs/{run_id}/steps/{step_id}")
  Single<RunStep> retrieveRunStep(@Path("thread_id") String var1, @Path("run_id") String var2, @Path("step_id") String var3);

  @Headers({"OpenAI-Beta: assistants=v1"})
  @GET("/v1/threads/{thread_id}/runs/{run_id}/steps")
  Single<OpenAiResponse<RunStep>> listRunSteps(@Path("thread_id") String var1, @Path("run_id") String var2, @QueryMap Map<String, String> var3);
}

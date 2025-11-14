package com.odde.doughnut.services;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.CharacterBasedTokenCountingStrategy;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
  private final OpenAiApi openAiApi;
  private static final int BATCH_SIZE = 64;
  private static final int MAX_TOKENS_PER_INPUT = 4000; // per-item token cap
  private static final String EMBEDDING_MODEL = "text-embedding-3-small";

  public EmbeddingService(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  /**
   * Stream embeddings for a stream of notes. Notes are batched internally with a batch size of 64
   * to reduce API round-trips. The output stream preserves input order and emits one result per
   * note.
   */
  public Stream<EmbeddingForNote> streamEmbeddingsForNotes(Stream<Note> notes) {
    final var noteIterator = notes.iterator();
    final Deque<EmbeddingForNote> buffer = new ArrayDeque<>();

    var iterator =
        new java.util.Iterator<EmbeddingForNote>() {
          @Override
          public boolean hasNext() {
            fillBufferIfNeeded();
            return !buffer.isEmpty();
          }

          @Override
          public EmbeddingForNote next() {
            fillBufferIfNeeded();
            return buffer.removeFirst();
          }

          private void fillBufferIfNeeded() {
            if (!buffer.isEmpty()) return;
            if (!noteIterator.hasNext()) return;

            List<Note> batch = new ArrayList<>(BATCH_SIZE);
            while (noteIterator.hasNext() && batch.size() < BATCH_SIZE) {
              batch.add(noteIterator.next());
            }

            List<String> inputs =
                batch.stream().map(EmbeddingService.this::combineNoteContent).toList();

            EmbeddingRequest request =
                EmbeddingRequest.builder().model(EMBEDDING_MODEL).input(inputs).build();

            EmbeddingResult result = blockGet(openAiApi.createEmbeddings(request));

            if (result != null
                && result.getData() != null
                && result.getData().size() == batch.size()) {
              for (int i = 0; i < batch.size(); i++) {
                @SuppressWarnings("unchecked")
                List<Float> embedding = (List<Float>) result.getData().get(i).getEmbedding();
                buffer.addLast(new EmbeddingForNote(batch.get(i), Optional.of(embedding)));
              }
            } else {
              // Fallback: emit empty optionals to keep alignment and progress
              for (Note n : batch) {
                buffer.addLast(new EmbeddingForNote(n, Optional.empty()));
              }
            }
          }
        };

    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }

  /** Convenience wrapper when a List is provided. */
  public Stream<EmbeddingForNote> streamEmbeddingsForNoteList(List<Note> notes) {
    return streamEmbeddingsForNotes(notes.stream());
  }

  /** Generate an embedding vector for a free-form search query. */
  public List<Float> generateQueryEmbedding(String query) {
    String input =
        new CharacterBasedTokenCountingStrategy()
            .truncateByApproxTokens(query == null ? "" : query.trim(), MAX_TOKENS_PER_INPUT);
    EmbeddingRequest request =
        EmbeddingRequest.builder().model(EMBEDDING_MODEL).input(List.of(input)).build();
    EmbeddingResult result = blockGet(openAiApi.createEmbeddings(request));
    if (result != null && result.getData() != null && !result.getData().isEmpty()) {
      @SuppressWarnings("unchecked")
      List<Float> embedding = (List<Float>) result.getData().get(0).getEmbedding();
      return embedding;
    }
    return List.of();
  }

  private String combineNoteContent(Note note) {
    String title = note.getTopicConstructor() != null ? note.getTopicConstructor() : "";
    String details = note.getDetails() != null ? note.getDetails() : "";

    // Build ancestor path like: A/B/C
    String ancestorPath =
        note.getAncestors().stream()
            .map(n -> n.getTopicConstructor() == null ? "" : n.getTopicConstructor())
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.joining(" \u203A "));

    StringBuilder sb = new StringBuilder(256);
    if (!ancestorPath.isBlank()) {
      sb.append("Context: ").append(ancestorPath).append('\n');
    }
    sb.append("Title: ").append(title).append('\n');
    sb.append("Details:\n");
    if (!details.isBlank()) {
      sb.append(details);
    }

    String structured = sb.toString();
    return new CharacterBasedTokenCountingStrategy()
        .truncateByApproxTokens(structured, MAX_TOKENS_PER_INPUT);
  }

  public static class EmbeddingForNote {
    private final Note note;
    private final Optional<List<Float>> embedding;

    public EmbeddingForNote(Note note, Optional<List<Float>> embedding) {
      this.note = note;
      this.embedding = embedding;
    }

    public Note note() {
      return note;
    }

    public Optional<List<Float>> embedding() {
      return embedding;
    }
  }
}

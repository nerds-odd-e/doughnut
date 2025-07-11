package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisVectorService {
  private final RedisTemplate<String, Object> redisTemplate;

  public RedisVectorService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storeEmbedding(Note note, List<Float> embedding) {
    String embeddingKey = getEmbeddingKey(note.getId());
    String notebookKey = getNotebookKey(note.getNotebook().getId());

    Map<String, Object> embeddingData = new HashMap<>();
    embeddingData.put("vector", embedding);
    embeddingData.put("noteId", note.getId());
    embeddingData.put("notebookId", note.getNotebook().getId());
    embeddingData.put("title", note.getTopicConstructor());
    embeddingData.put("createdAt", ZonedDateTime.now().toString());
    embeddingData.put("isChunk", false);
    embeddingData.put("chunkIndex", 0);

    redisTemplate.opsForHash().putAll(embeddingKey, embeddingData);
    redisTemplate.opsForSet().add(notebookKey, note.getId());
  }

  public void deleteEmbedding(Integer noteId) {
    String embeddingKey = getEmbeddingKey(noteId);
    redisTemplate.delete(embeddingKey);
  }

  public void deleteNotebookEmbeddings(Integer notebookId) {
    String notebookKey = getNotebookKey(notebookId);
    Set<Object> noteIds = redisTemplate.opsForSet().members(notebookKey);

    if (noteIds != null) {
      for (Object noteId : noteIds) {
        deleteEmbedding(Integer.valueOf(noteId.toString()));
      }
    }

    redisTemplate.delete(notebookKey);
  }

  private String getEmbeddingKey(Integer noteId) {
    return "embedding:" + noteId;
  }

  private String getNotebookKey(Integer notebookId) {
    return "notebook:" + notebookId + ":notes";
  }
}

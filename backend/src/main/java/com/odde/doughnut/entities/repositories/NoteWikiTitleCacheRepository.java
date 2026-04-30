package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteWikiTitleCache;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteWikiTitleCacheRepository extends JpaRepository<NoteWikiTitleCache, Integer> {

  void deleteByNote_Id(Integer noteId);

  List<NoteWikiTitleCache> findByNote_IdOrderByIdAsc(Integer noteId);
}

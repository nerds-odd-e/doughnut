package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AuthoredNoteReferenceBackfillProgress;
import org.springframework.data.jpa.repository.JpaRepository;

/** The single (id = 1) {@link AuthoredNoteReferenceBackfillProgress} row's repository. */
public interface AuthoredNoteReferenceBackfillProgressRepository
    extends JpaRepository<AuthoredNoteReferenceBackfillProgress, Integer> {

  int SINGLETON_ID = 1;
}

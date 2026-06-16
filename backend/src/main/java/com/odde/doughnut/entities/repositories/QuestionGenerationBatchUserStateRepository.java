package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionGenerationBatchUserStateRepository
    extends JpaRepository<QuestionGenerationBatchUserState, Integer> {

  Optional<QuestionGenerationBatchUserState> findByUser_Id(Integer userId);
}

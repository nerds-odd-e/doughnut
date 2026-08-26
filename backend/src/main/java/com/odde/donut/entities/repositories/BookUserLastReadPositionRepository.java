package com.odde.donut.entities.repositories;

import com.odde.donut.entities.BookUserLastReadPosition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookUserLastReadPositionRepository
    extends JpaRepository<BookUserLastReadPosition, Integer> {

  Optional<BookUserLastReadPosition> findByUser_IdAndBook_Id(Integer userId, Integer bookId);

  void deleteByBook_Id(Integer bookId);
}

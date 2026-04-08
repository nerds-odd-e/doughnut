package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BookRangeReadingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRangeReadingRecordRepository
    extends JpaRepository<BookRangeReadingRecord, Integer> {

  Optional<BookRangeReadingRecord> findByUser_IdAndBookRange_Id(
      Integer userId, Integer bookRangeId);

  List<BookRangeReadingRecord> findAllByUser_IdAndBookRange_Book_Id(Integer userId, Integer bookId);
}

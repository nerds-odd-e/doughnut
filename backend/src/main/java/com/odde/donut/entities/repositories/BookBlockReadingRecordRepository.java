package com.odde.donut.entities.repositories;

import com.odde.donut.entities.BookBlockReadingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookBlockReadingRecordRepository
    extends JpaRepository<BookBlockReadingRecord, Integer> {

  Optional<BookBlockReadingRecord> findByUser_IdAndBookBlock_Id(
      Integer userId, Integer bookBlockId);

  List<BookBlockReadingRecord> findAllByUser_IdAndBookBlock_Book_Id(Integer userId, Integer bookId);
}

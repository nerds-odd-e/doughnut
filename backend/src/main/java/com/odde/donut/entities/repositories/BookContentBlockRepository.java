package com.odde.donut.entities.repositories;

import com.odde.donut.entities.BookContentBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookContentBlockRepository extends JpaRepository<BookContentBlock, Integer> {
  List<BookContentBlock> findAllByBookBlock_IdOrderBySiblingOrder(Integer bookBlockId);
}

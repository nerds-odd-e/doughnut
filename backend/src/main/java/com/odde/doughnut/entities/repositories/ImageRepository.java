package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Image;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ImageRepository extends CrudRepository<Image, Integer> {

  List<Image> findByNote_Id(Integer noteId);
}

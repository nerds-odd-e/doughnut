package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Image;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ImageRepository extends CrudRepository<Image, Integer> {

  List<Image> findByNote_Id(Integer noteId);
}

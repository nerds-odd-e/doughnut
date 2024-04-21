package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AttachmentBlob;
import org.springframework.data.repository.CrudRepository;

public interface ImageBlobRepository extends CrudRepository<AttachmentBlob, Integer> {}

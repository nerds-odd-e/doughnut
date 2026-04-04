package com.odde.doughnut.services.book;

import com.odde.doughnut.entities.AttachmentBlob;
import com.odde.doughnut.entities.repositories.AttachmentBlobRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbBookPdfStorage implements BookPdfStorage {

  private final AttachmentBlobRepository attachmentBlobRepository;

  public DbBookPdfStorage(AttachmentBlobRepository attachmentBlobRepository) {
    this.attachmentBlobRepository = attachmentBlobRepository;
  }

  @Override
  @Transactional
  public String put(byte[] data) {
    AttachmentBlob blob = new AttachmentBlob();
    blob.setData(data);
    attachmentBlobRepository.save(blob);
    return String.valueOf(blob.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<byte[]> get(String ref) {
    int id;
    try {
      id = Integer.parseInt(ref.trim());
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
    if (id <= 0) {
      return Optional.empty();
    }
    return attachmentBlobRepository.findById(id).map(AttachmentBlob::getData);
  }
}

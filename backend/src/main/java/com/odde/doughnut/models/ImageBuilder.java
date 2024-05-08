package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.ImageUtils;
import com.odde.doughnut.entities.AttachmentBlob;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.User;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public class ImageBuilder {
  private final ImageUtils imageUtils = new ImageUtils();

  public ImageBuilder() {}

  public Image buildImageFromUploadedPicture(User user, MultipartFile file) throws IOException {
    Image image = new Image();
    image.setUser(user);
    image.setName(file.getOriginalFilename());
    image.setType(file.getContentType());
    AttachmentBlob attachmentBlob = getImageBlob(file);
    image.setBlob(attachmentBlob);
    return image;
  }

  private AttachmentBlob getImageBlob(MultipartFile file) throws IOException {
    byte[] data = imageUtils.toResizedImageByteArray(file, file.getOriginalFilename());
    AttachmentBlob attachmentBlob = new AttachmentBlob();
    attachmentBlob.setData(data);
    return attachmentBlob;
  }
}

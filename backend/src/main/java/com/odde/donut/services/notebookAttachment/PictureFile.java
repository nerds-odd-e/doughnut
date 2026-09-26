package com.odde.donut.services.notebookAttachment;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * A notebook file is a picture by its name's extension, ignoring case. Uploads are admitted by that
 * name alone (the declared content type and the bytes are not checked) up to {@link #LIMIT_BYTES}.
 */
public final class PictureFile {
  public static final long LIMIT_BYTES = 10 * 1024 * 1024;

  private static final Map<String, MediaType> TYPES =
      Map.of(
          "png", MediaType.IMAGE_PNG,
          "jpg", MediaType.IMAGE_JPEG,
          "jpeg", MediaType.IMAGE_JPEG,
          "gif", MediaType.IMAGE_GIF,
          "webp", MediaType.parseMediaType("image/webp"));

  private PictureFile() {}

  public static Optional<MediaType> mediaType(String name) {
    return Optional.ofNullable(StringUtils.getFilenameExtension(name))
        .map(extension -> TYPES.get(extension.toLowerCase(Locale.ROOT)));
  }

  public static void admit(String name, long size) {
    if (mediaType(name).isEmpty()) {
      throw refused(
          "Cannot upload " + name + ": a picture must be a png, jpg, jpeg, gif or webp file.");
    }
    if (size > LIMIT_BYTES) {
      throw refused("File size exceeds the limit: " + LIMIT_BYTES + " bytes.");
    }
  }

  private static ApiException refused(String message) {
    return new ApiException(message, ApiError.ErrorType.BINDING_ERROR, message);
  }
}

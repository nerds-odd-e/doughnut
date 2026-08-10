package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import com.odde.doughnut.validators.NotBlankDisplayName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Rename a folder in place within its current parent.")
@Getter
@Setter
public class FolderRenameRequest {

  @NotBlankDisplayName
  @Size(max = 512)
  @Pattern(regexp = DisplayNamePathSeparators.REGEXP, message = DisplayNamePathSeparators.MESSAGE)
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "New display name for the folder")
  private String name;
}

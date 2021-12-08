package com.odde.doughnut.entities;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.entities.validators.ValidateNotePicture;
import com.odde.doughnut.models.ImageBuilder;

import org.apache.logging.log4j.util.Strings;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@ValidateNotePicture
public class NoteContent {
    @Column(name = "id", insertable = false, updatable = false)
    @Getter
    private Integer id;

    @NotEmpty
    @Size(min = 1, max = 100)
    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    @Column(name = "title_idn", nullable = true)
    private String titleIDN;

    @Getter
    @Setter
    @Column(name = "description_idn", nullable = true)
    private String descriptionIDN;

    @Getter
    @Setter
    private String url;

    @Column(name = "url_is_video")
    @Getter
    @Setter
    private Boolean urlIsVideo = false;

    @Column(name="picture_url")
    @Getter @Setter private String pictureUrl;

    @Pattern(regexp="^(((-?[0-9.]+\\s+){3}-?[0-9.]+\\s+)*((-?[0-9.]+\\s+){3}-?[0-9.]+))?$",message="must be 'x y width height [x y width height...]'")
    @Column(name = "picture_mask")
    @Getter
    @Setter
    private String pictureMask;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "image_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private Image uploadPicture;

    @Column(name = "use_parent_picture")
    @Getter
    @Setter
    private Boolean useParentPicture = false;

    @Column(name = "skip_review")
    @Getter
    @Setter
    private Boolean skipReview = false;

    @JsonIgnore
    @Transient @Getter @Setter private MultipartFile uploadPictureProxy;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Transient @Getter @Setter private String testingParent;

    @Column(name = "updated_at")
    @Getter
    @Setter
    private Timestamp updatedAt;

    @Column(name = "is_outdated_translation_idn")
    @Getter
    @Setter
    private Boolean isTranslationOutdatedIDN;

    @JsonIgnore
    public String getNotePicture() {
        if (uploadPicture != null) {
            return "/images/" + uploadPicture.getId() + "/" + uploadPicture.getName();
        }
        return pictureUrl;
    }

    public String getPictureMaskSvg(String opacity) {
        if(Strings.isEmpty(pictureMask)) {
            return "";
        }
        List<String> list = Arrays.stream(pictureMask.split("\\s+")).collect(Collectors.toUnmodifiableList());
        List<String> results = new ArrayList<>();
        for (int i = 0; i < list.size(); i+=4) {
            results.add(String.format("<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" style=\"fill:blue;stroke:pink;stroke-width:1;fill-opacity:%s;stroke-opacity:0.8\" />", list.get(i), list.get(i+1), list.get(i+2), list.get(i+3), opacity));
        }

        return String.join("", results);
    }

    public boolean hasPicture() {
        return Strings.isNotBlank(pictureUrl) || uploadPicture != null || useParentPicture;
    }

    public void fetchUploadedPicture(User user) throws IOException {
        MultipartFile file = getUploadPictureProxy();
        if (file != null && !file.isEmpty()) {
            Image image = new ImageBuilder().buildImageFromUploadedPicture(user, file);
            setUploadPicture(image);
        }
    }

    @JsonIgnore
    public String getClozeDescription() {
        if(Strings.isEmpty(description)) return "";

        return ClozeDescription.htmlClosedDescription().getClozeDescription(getNoteTitle(), description);
    }

    @JsonIgnore
    public NoteTitle getNoteTitle() {
        return new NoteTitle(title);
    }

    @JsonIgnore
    public String getShortDescription() {
        return StringUtils.abbreviate(description, 50);
    }

    @JsonIgnore
    public String getShortDescriptionIDN() {
        return StringUtils.abbreviate(descriptionIDN, 50);
    }
}

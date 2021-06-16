package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.entities.validators.ValidateNotePicture;
import com.odde.doughnut.models.ImageBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Embeddable
@ValidateNotePicture
public class NoteContent {
    @Column(name = "id", insertable = false, updatable = false)
    @Getter
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

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

    @Column(name = "hide_title_in_article")
    @Getter
    @Setter
    private Boolean hideTitleInArticle = false;

    @Column(name = "show_as_bullet_in_article")
    @Getter
    @Setter
    private Boolean showAsBulletInArticle = false;

    @JsonIgnore
    @Transient @Getter @Setter private MultipartFile uploadPictureProxy;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Transient @Getter @Setter private String testingParent;

    @Column(name = "updated_at")
    @Getter
    @Setter
    private Timestamp updatedAt = new Timestamp(System.currentTimeMillis());

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
        return new ClozeDescription(
                "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
                "<mark title='Hidden text that is matching the answer'>[...]</mark>",
                "<mark title='Hidden pronunciation'>/.../</mark>"
        ).getClozeDescription(getNoteTitle(), description);
    }

    @JsonIgnore
    public NoteTitle getNoteTitle() {
        return new NoteTitle(title);
    }

    @JsonIgnore
    public String getShortDescription() {
        return StringUtils.abbreviate(description, 50);
    }
}

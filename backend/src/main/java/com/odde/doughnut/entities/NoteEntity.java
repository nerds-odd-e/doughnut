package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
public class NoteEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String picture;
    @Getter
    @Setter
    private String url;

    @Column(name = "url_is_video")
    @Getter
    @Setter
    private Boolean urlIsVideo = false;

    @Column(name = "skip_review")
    @Getter
    @Setter
    private Boolean skipReview = false;

    @Column(name = "sibling_order")
    @Getter
    @Setter
    private Long siblingOrder = getGoodEnoughOrderNumber();


    //
    // SiblingOrder is used to decide the order among the notes under the same parent.
    // In a real life situation, using millisecond * 1000 is good enough for ordering and
    // also leave enough space for inserting items in between.
    // However, in extreme cases like unit testing or importing batch of notes, there is
    // still a chance of duplicated order number.
    // Since this will likely to happy within the same java running instance, a simple
    // static variable `localLastOrderNumberForGoodEnoughSiblingOrder` is introduced to
    // detect and remove the duplicates.
    //
    static long localLastOrderNumberForGoodEnoughSiblingOrder;
    public static final long MINIMUM_SIBLING_ORDER_INCREMENT = 1000;

    private static Long getGoodEnoughOrderNumber() {
        long newNumber = System.currentTimeMillis() * MINIMUM_SIBLING_ORDER_INCREMENT;
        if (newNumber <= localLastOrderNumberForGoodEnoughSiblingOrder) {
            localLastOrderNumberForGoodEnoughSiblingOrder += MINIMUM_SIBLING_ORDER_INCREMENT;
        } else {
            localLastOrderNumberForGoodEnoughSiblingOrder = newNumber;
        }
        return localLastOrderNumberForGoodEnoughSiblingOrder;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    @Getter
    @Setter
    private NoteEntity parentNote;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ownership_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private OwnershipEntity ownershipEntity;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
    @Getter
    @Setter
    private ReviewSettingEntity masterReviewSettingEntity;

    @Column(name = "parent_id", insertable = false, updatable = false)
    @Getter
    private Integer parentId;

    @Column(name = "user_id", insertable = false, updatable = false)
    @Getter
    private Integer userId;

    @OneToMany(mappedBy = "parentNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sibling_order")
    @JsonIgnore
    @Getter
    private final List<NoteEntity> children = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private UserEntity userEntity;

    @Column(name = "created_datetime")
    @Getter
    @Setter
    private Date createdDatetime;

    @Column(name = "updated_datetime")
    @Getter
    @Setter
    private Date updatedDatetime;

    @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<LinkEntity> links = new ArrayList<>();

    @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<LinkEntity> refers = new ArrayList<>();

    @Transient
    @Getter
    @Setter
    private String testingParent;

    public List<NoteEntity> getTargetNotes() {
        return links.stream().map(LinkEntity::getTargetNote).collect(toList());
    }

    public List<String> linkTypes() {
        List<String> referredTypes = refers.stream().map(LinkEntity::getType).filter(t -> t.equals(LinkEntity.LinkType.RELATED_TO.label)).collect(toList());
        List<String> linkedTypes = links.stream().map(LinkEntity::getType).collect(toList());
        referredTypes.addAll(linkedTypes);
        return referredTypes.stream().distinct().collect(Collectors.toUnmodifiableList());
    }

    public List<NoteEntity> linksOfType(String type) {
        List<NoteEntity> linkedNotes = links.stream().filter(l -> l.getType().equals(type)).map(LinkEntity::getTargetNote).collect(Collectors.toList());
        if (type.equals(LinkEntity.LinkType.RELATED_TO.label)) {
            List<NoteEntity> referredNotes = refers.stream().filter(l -> l.getType().equals(type)).map(LinkEntity::getSourceNote).collect(Collectors.toUnmodifiableList());
            linkedNotes.addAll(referredNotes);
        }
        return linkedNotes;
    }

    public boolean isFromCircle() {
        return circle() != null;
    }

    public CircleEntity circle() {
        return ownershipEntity.getCircleEntity();
    }

    public String getClozeDescription() {
        return Arrays.stream(title.split("/"))
                .map(String::trim)
                .reduce(description, this::clozeString);
    }

    private String clozeString(String description, String wordToHide) {
        String ptn = String.join("([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?",
                Arrays.stream(wordToHide.split("[\\s-]+"))
                        .filter(x->!Arrays.asList("the", "a", "an").contains(x))
                        .map(Pattern::quote).collect(Collectors.toUnmodifiableList()));
        Pattern pattern = Pattern.compile(ptn, Pattern.CASE_INSENSITIVE);
        String literal = pattern.matcher(description).replaceAll("[...]");
        String substring = wordToHide.substring(0, wordToHide.length() * 3 / 4);
        pattern = Pattern.compile(Pattern.quote(substring), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(literal).replaceAll("[..~]");
    }

}



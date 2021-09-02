<template>
  <div class="review-point-abbr">
    <span v-if="!!noteViewedByUser">
      {{ noteViewedByUser.note.noteContent.title }}
    </span>

    <span v-if="!!linkViewedByUser">
      <span>
        {{ linkViewedByUser.sourceNoteViewedByUser.note.noteContent.title }}
      </span>
      <span class="badge mr-1"> {{ linkViewedByUser.linkTypeLabel }}</span>
      <span>
        {{ linkViewedByUser.targetNoteViewedByUser.note.noteContent.title }}
      </span>
    </span>
  </div>
</template>

<script>
import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue";
import NoteViewedByUserWithoutChildren from "../notes/NoteViewedByUserWithoutChildren.vue";
import LinkShow from "../links/LinkShow.vue";

export default {
  name: "ShowReviewPoint",
  props: { noteViewedByUser: Object, linkViewedByUser: Object },
  emits: ["updated"],
  components: {
    NoteBreadcrumbForReview,
    NoteViewedByUserWithoutChildren,
    LinkShow,
  },
};
</script>

<style lang="sass" scoped>
.review-point-abbr
    font-size: small
    color: white
</style>

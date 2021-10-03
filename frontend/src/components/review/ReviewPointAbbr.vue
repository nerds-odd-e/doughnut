<template>
  <div class="review-point-abbr">
    <span v-if="!!noteWithPosition">
      {{ noteWithPosition.note.title }}
    </span>

    <span v-if="!!linkViewedByUser">
      <span>
        {{ linkViewedByUser.sourceNoteWithPosition.note.title }}
      </span>
      <span class="badge mr-1"> {{ linkViewedByUser.linkTypeLabel }}</span>
      <span>
        {{ linkViewedByUser.targetNoteWithPosition.note.title }}
      </span>
    </span>
  </div>
</template>

<script>
import NoteWithControls from "../notes/NoteWithControls.vue";
import LinkShow from "../links/LinkShow.vue";

export default {
  name: "ShowReviewPoint",
  props: { noteWithPosition: Object, linkViewedByUser: Object },
  components: {
    NoteWithControls,
    LinkShow,
  },
};
</script>

<style lang="sass" scoped>
.review-point-abbr
    font-size: small
    color: white
</style>

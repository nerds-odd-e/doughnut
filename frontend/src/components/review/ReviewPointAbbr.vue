<template>
  <div class="review-point-abbr">
    <span v-if="note">
      {{ noteTitle }}
    </span>

    <span v-if="!!linkViewedByUser">
      <span>
        {{ sourceNoteTitle }}
      </span>
      <span class="badge mr-1"> {{ linkViewedByUser.linkTypeLabel }}</span>
      <span>
        {{ targetNoteTitle }}
      </span>
    </span>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    reviewPointViewedByUser: {
      type: Object as PropType<Generated.ReviewPointViewedByUser>,
      required: true,
    },
  },
  computed: {
    note() {
      return this.reviewPointViewedByUser?.reviewPoint?.thing?.note;
    },
    linkViewedByUser() {
      return this.reviewPointViewedByUser?.linkViewedByUser;
    },
    noteTitle() {
      return this.note?.title;
    },
    sourceNoteTitle() {
      return this.linkViewedByUser?.sourceNoteWithPosition.note.note.title;
    },
    targetNoteTitle() {
      return this.linkViewedByUser?.targetNoteWithPosition.note.note.title;
    },
  },
});
</script>

<style lang="sass" scoped>
.review-point-abbr
    font-size: small
    color: white
</style>

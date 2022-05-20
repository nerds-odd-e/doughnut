<template>
  <div class="review-point-abbr">
    <span v-if="note">
      {{ noteTitle }}
    </span>

    <span v-if="link">
      <span>
        {{ sourceNoteTitle }}
      </span>
      <span class="badge mr-1"> {{ link.linkTypeLabel }}</span>
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
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
  },
  computed: {
    note() {
      return this.reviewPoint?.thing.note;
    },
    link() {
      return this.reviewPoint?.thing.link;
    },
    noteTitle() {
      return this.note?.title;
    },
    sourceNoteTitle() {
      return this.link?.sourceNote.title;
    },
    targetNoteTitle() {
      return this.link?.targetNote.title;
    },
  },
});
</script>

<style lang="sass" scoped>
.review-point-abbr
    font-size: small
    color: white
</style>

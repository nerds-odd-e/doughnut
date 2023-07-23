<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>
    <span v-if="note">
      {{ noteTitle }}
    </span>

    <span v-if="link">
      <span>
        {{ sourceNoteTitle }}
      </span>
      <span class="badge mr-1"> {{ link.linkType }}</span>
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
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 2px
</style>

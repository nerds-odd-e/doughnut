<template>
  <div class="review-point-abbr">
    <span v-if="!!noteWithPosition">
      {{ noteTitle }}
    </span>

    <span v-if="!!linkViewedByUser">
      <span>
        {{ sourceNoteTitle }}
      </span>
      <span class="badge mr-1"> {{ linkViewedByUser.linkTypeLabel }}</span>
      <span>
        {{ sourceNoteTitle }}
      </span>
    </span>
  </div>
</template>

<script lang="ts">
import { PropType } from 'vue-demi';

export default {
  name: "ShowReviewPoint",
  props: {
    noteWithPosition: Object as PropType<Generated.NoteWithPosition>,
    linkViewedByUser: Object as PropType<Generated.LinkViewedByUser>
  },
  computed: {
    noteTitle() {
      return this.noteWithPosition?.note.note.title
    },
    sourceNoteTitle() {
      return this.linkViewedByUser?.sourceNoteWithPosition.note.note.title
    },
    targetNoteTitle() {
      return this.linkViewedByUser?.targetNoteWithPosition.note.note.title
    }

  }
};
</script>

<style lang="sass" scoped>
.review-point-abbr
    font-size: small
    color: white
</style>

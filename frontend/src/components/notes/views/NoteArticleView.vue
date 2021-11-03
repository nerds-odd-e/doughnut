<template>
  <div class="container">
    <NoteOverview
      v-bind="{ noteId, highlightNoteId, expandChildren }"
      :key="noteId"
      @highlight="$emit('highlight', $event)"
    />
  </div>
</template>

<script lang="ts">
import NoteOverview from "../NoteOverview.vue";

export default {
  props: {
    highlightNoteId: [String, Number],
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
  },
  emits: ['highlight'],
  components: { NoteOverview },
  computed: {
  },
};
</script>

<style lang="sass" scoped>

::v-deep .note-body
  border-width: 0px
  border-left-width: 3px

</style>
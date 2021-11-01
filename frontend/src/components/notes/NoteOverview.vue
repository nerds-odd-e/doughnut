<template>
  <NoteWithLinks v-bind="{ ...noteViewedByUser }"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in childrenIds"
      v-bind="{ noteId: childId, highlightNoteId }"
      :key="childId"
      @highlight="$emit('highlight', $event)"
    />
  </div>
</template>

<script lang="ts">
import NoteWithLinks from "./NoteWithLinks.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: [String, Number],
    highlightNoteId: [String, Number],
  },
  emits: ["highlight"],
  components: { NoteWithLinks },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.$store.getters.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

<style lang="sass" scoped>
.note-list
  margin-left: 10px
</style>

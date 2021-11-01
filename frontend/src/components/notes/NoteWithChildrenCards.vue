<template>
  <template v-if="note">
    <NoteWithLinks v-bind="note"/>
    <Cards v-if="expandChildrenCards" :notes="children"/>
  </template>

</template>

<script>
import NoteWithLinks from "./NoteWithLinks.vue";
import Cards from "./Cards.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    noteId: [String, Number],
    highlightNoteId: [String, Number],
    expandChildrenCards: { type: Boolean, default: true },
  },
  emits: ['highlight'],
  components: {
    NoteWithLinks,
    Cards,
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.noteId);
    },
  },
};
</script>

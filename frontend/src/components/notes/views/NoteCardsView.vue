<template>
  <div class="container" v-if="note">
    <NoteWithLinks v-bind="note"/>
    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script>
import NoteWithLinks from "../NoteWithLinks.vue";
import Cards from "../Cards.vue";

export default {
  props: {
    noteId: [String, Number],
    highlightNoteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
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

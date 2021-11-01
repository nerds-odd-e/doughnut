<template>
  <template v-if="note">
    <NoteWithLinks v-bind="note"/>
    <Cards :notes="children"/>
    <router-link
      :to="{ name: 'noteOverview', params: { noteId: noteId } }"
      role="button"
      class="btn btn-sm"
    >
      Full view mode
    </router-link>

    <router-link
      :to="{ name: 'mindmap', params: { noteId: noteId } }"
      role="button"
      class="btn btn-sm"
    >
      Mindmap mode
    </router-link>
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

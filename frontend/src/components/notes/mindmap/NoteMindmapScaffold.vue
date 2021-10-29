<template>
  <component v-bind:is="noteComponent" v-bind="{
    note: noteViewedByUser,
    mindmapSector,
    mindmap,
    highlighted: highlightNoteId === noteViewedByUser.id

  }"/>
  <NoteMindmapScaffold
    v-for="(childId, index) in childrenIds"
    v-bind="{
      noteComponent,
      noteId: childId,
      mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
      mindmap,
      highlighted: highlightNoteId === childId
    }"
    :key="childId"
  />
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import NoteCard from "./NoteCard.vue";
import NoteParentChildConnection from "./NoteParentChildConnection.vue";
import NoteLinks from "./NoteLinks.vue";

export default {
  name: "NoteMindmap",
  props: {
    noteId: [String, Number],
    mindmapSector: MindmapSector,
    mindmap: Object,
    noteComponent: String,
  },
  components: { NoteCard, NoteParentChildConnection, NoteLinks },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.$store.getters.getChildrenIdsByParentId(this.noteId);
    },
    highlightNoteId() { return this.$store.getters.getHighlightNoteId() },

  },
};
</script>

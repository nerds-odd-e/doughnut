<template>
  <component v-bind:is="noteComponent" v-bind="{
    note: noteViewedByUser,
    scale,
    mindmapSector: mindmapSector,
    rootNoteId: !!rootNoteId ? rootNoteId : noteId,
    rootMindmapSector: !!rootMindmapSector ? rootMindmapSector : mindmapSector

  }"/>
  <NoteMindmapScaffold
    v-for="(childId, index) in childrenIds"
    v-bind="{
      noteComponent,
      scale,
      noteId: childId,
      mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
      rootNoteId: !!rootNoteId ? rootNoteId : noteId,
      rootMindmapSector: !!rootMindmapSector ? rootMindmapSector : mindmapSector
    }"
    :key="childId"
  />
</template>

<script lang="ts">
import MindmapSector from "@/models/MindmapSector";
import NoteCard from "./NoteCard.vue";
import NoteConnection from "./NoteConnection.vue";

export default {
  name: "NoteMindmap",
  props: {
    noteId: [String, Number],
    scale: Number,
    mindmapSector: MindmapSector,
    noteComponent: String,
    rootNoteId: [Number, String],
    rootMindmapSector: MindmapSector,
  },
  components: { NoteCard, NoteConnection },
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

<template>
  <NoteCard v-is="noteComponent" v-bind="{ note: noteViewedByUser, mindmapSector: mindmapSector }"/>
  <NoteMindmapScaffold
    v-for="(childId, index) in childrenIds"
    v-bind="{ noteComponent, noteId: childId, mindmapSector: mindmapSector.getChildSector(childrenIds.length, index) }"
    :key="childId"
  />
</template>

<script lang="ts">
import MindmapSector from "@/models/MindmapSector";
import NoteCard from "./NoteCard.vue";

export default {
  name: "NoteMindmap",
  props: {
    noteId: Number,
    mindmapSector: MindmapSector,
    noteComponent: String,
  },
  emits: ["updated"],
  components: { NoteCard },
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

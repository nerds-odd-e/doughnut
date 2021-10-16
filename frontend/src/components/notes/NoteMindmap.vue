<template>
  <NoteCard v-bind="{ note: noteViewedByUser, mindmapSector: mindmapSector }"/>
  <NoteMindmap
    v-for="(childId, index) in childrenIds"
    v-bind="{ noteId: childId, mindmapSector: mindmapSector.getChildSector(childrenIds.length, index) }"
    :key="childId"
  />
</template>

<script lang="ts">
import NoteCard from "./NoteCard.vue";
import MindmapSector from "./MindmapSector";

export default {
  name: "NoteOverview",
  props: {
    noteId: Number,
    mindmapSector: { type: MindmapSector, default: new MindmapSector(0, 0, 0, 360)}
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

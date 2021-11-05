<template>
  <template v-if="note">
    <slot v-bind="{
        note,
        mindmapSector,
        mindmap,
      }"/>
    <NoteMindmapScaffold
      v-for="(childId, index) in childrenIds"
      v-bind="{
        highlightNoteId,
        noteId: childId,
        mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
        mindmap,
      }"
      :key="childId"
    >
      <template #default="{note, mindmapSector, mindmap}">
        <slot v-bind="{
          note,
          mindmapSector,
          mindmap,}"/>
      </template>
    </NoteMindmapScaffold>
  </template>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";

export default {
  name: "NoteMindmap",
  props: {
    noteId: [String, Number],
    mindmapSector: MindmapSector,
    mindmap: Object,
    highlightNoteId: [String, Number]
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.$store.getters.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

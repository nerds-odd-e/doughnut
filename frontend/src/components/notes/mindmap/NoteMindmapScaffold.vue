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
        noteComponent,
        noteId: childId,
        mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
        mindmap,
      }"
      :key="childId"
      @highlight="$emit('highlight', $event)"
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
    noteComponent: String,
    highlightNoteId: [String, Number]
  },
  emits: ['highlight'],
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

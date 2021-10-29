<template>
  <template v-if="ancestors.length > 0">
  <component v-bind:is="noteComponent" v-bind="{
      note: noteViewedByUser,
      mindmap,
      mindmapSector: mindmapSector.getChildSector(1, 0, 0.5),
      highlighted: highlightNoteId === noteViewedByUser.id
    }"
    @highlight="$emit('highlight', noteViewedByUser.id)"
  />
  <NoteMindmapAncestorsScaffold
    v-bind="{ highlightNoteId, noteComponent, mindmap, ancestors: ancestors.slice(0, ancestors.length - 1), mindmapSector: mindmapSector.getChildSector(1, 0, 0.5) }"
    @highlight="$emit('highlight', $event)"
  />
  </template>
</template>

<script lang="ts">
import MindmapSector from "@/models/MindmapSector";
import NoteCard from "./NoteCard.vue";
import NoteParentConnection from "./NoteParentConnection.vue";

export default {
  name: "NoteMindmap",
  props: {
    ancestors: Array,
    mindmapSector: MindmapSector,
    noteComponent: String,
    mindmap: Object,
    highlightNoteId: [String, Number]
  },
  emits: ['highlight'],
  components: { NoteCard, NoteParentConnection },
  computed: {
    noteViewedByUser() {
      return this.ancestors[this.ancestors.length - 1]
    },
  },
};
</script>

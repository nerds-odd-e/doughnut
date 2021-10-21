<template>
  <svg class="mindmap-canvas">
    <NoteMindmapAncestorsScaffold v-bind="{ ancestors, scale: scale / 2, mindmapSector: mindmapAncestorSector, noteComponent: 'NoteParentConnection'}"/>
    <NoteMindmapScaffold v-bind="{ noteId, scale, mindmapSector, noteComponent: 'NoteConnection'}"/>
  </svg>
  <NoteMindmapAncestorsScaffold v-bind="{ ancestors, scale: scale/2, mindmapSector: mindmapAncestorSector, noteComponent: 'NoteCard'}"/>
  <NoteMindmapScaffold v-bind="{ noteId, scale, mindmapSector, noteComponent: 'NoteCard'}"/>
</template>

<script lang="ts">
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import NoteMindmapAncestorsScaffold from "./NoteMindmapAncestorsScaffold.vue";
import MindmapSector from "@/models/MindmapSector";

export default {
  name: "NoteMindmap",
  props: {
    noteId: [String, Number],
    ancestors: Array,
    scale: Number,
  },
  emits: ["updated"],
  components: { NoteMindmapScaffold, NoteMindmapAncestorsScaffold },
  computed: {
    mindmapAncestorSector() {
      return new MindmapSector(0, 0, -Math.PI/2, 0)
    },
    mindmapSector() {
      var d = 0
      if (this.ancestors?.length > 0) d = Math.PI / 10
      return new MindmapSector(0, 0, -Math.PI / 2 + d, Math.PI * 2 - d * 2)
    },
  },
};
</script>

<style lang="sass" scoped>
.mindmap-canvas
  overflow: visible
</style>

<template>
  <svg class="mindmap-canvas">
    <marker id="arrowhead" markerWidth="8" markerHeight="6" 
    refX="8" refY="3" orient="auto">
      <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
    </marker>
    <marker id="treearrow" class="parent-child" markerWidth="4" markerHeight="3" 
    refX="3" refY="1.5" orient="auto">
      <polygon points="0 0, 4 1.5, 0 3" style="stroke-width:0"/>
    </marker>


    <NoteMindmapAncestorsScaffold v-bind="{ ancestors, mindmap, mindmapSector: mindmapAncestorSector, noteComponent: 'NoteParentConnection'}"/>
    <NoteMindmapScaffold v-bind="{ noteId, mindmap, mindmapSector, noteComponent: 'NoteConnection'}"/>
  </svg>
  <NoteMindmapAncestorsScaffold v-bind="{ ancestors, mindmap, mindmapSector: mindmapAncestorSector, noteComponent: 'NoteCard'}"/>
  <NoteMindmapScaffold v-bind="{ noteId, mindmap, mindmapSector, noteComponent: 'NoteCard'}"/>
</template>

<script lang="ts">
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import NoteMindmapAncestorsScaffold from "./NoteMindmapAncestorsScaffold.vue";
import MindmapSector from "@/models/MindmapSector";
import Mindmap from "@/models/Mindmap";

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
    mindmap() {
      return new Mindmap(
        this.scale,
        this.mindmapSector,
        this.noteId,
        this.$store.getters.getNoteById,
        150,
        50
      )
    },

  },
};
</script>

<style lang="sass">
.mindmap-canvas
  overflow: visible
.parent-child
  stroke: #FFccaa
  fill: #FFccaa

</style>

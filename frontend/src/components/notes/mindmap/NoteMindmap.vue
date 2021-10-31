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


    <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteParentChildConnection'}"/>
    <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteLinks'}"/>
  </svg>
  <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteCard'}" @highlight="highlight"/>
</template>

<script lang="ts">
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import MindmapSector from "@/models/MindmapSector";
import Mindmap from "@/models/Mindmap";

export default {
  name: "NoteMindmap",
  props: {
    highlightNoteId: [String, Number],
    noteId: [String, Number],
    scale: Number,
    rotate: Number,
  },
  emits: ['highlight'],
  components: { NoteMindmapScaffold },
  methods: {
    highlight(id) {this.$emit('highlight', id)}
  },
  computed: {
    mindmapSector() {
      return new MindmapSector(0, 0, this.rotate - Math.PI / 2, Math.PI * 2)
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

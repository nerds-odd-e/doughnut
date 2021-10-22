<template>
  <g style="stroke:green;fill:green" v-if="connection">
    <line v-bind="connection"
      style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import Mindmap from "@/models/Mindmap";



export default {

  props: {
    link: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    rootNoteId: [Number, String],
    rootMindmapSector: MindmapSector,
  },
  computed: {
    mindmap() {
      return new Mindmap(
        this.scale,
        this.rootMindmapSector,
        this.rootNoteId,
        this.$store.getters.getNoteById
      )},

    connection() {
      return this.mindmap.connection(this.mindmapSector, this.link.targetNote.id)
    },

  },
}


</script>

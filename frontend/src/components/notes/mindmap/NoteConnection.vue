<template>
  <g style="stroke:#FF6700;fill:#FF6700">
      <marker id="arrowhead" markerWidth="8" markerHeight="6" 
      refX="8" refY="3" orient="auto">
        <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
      </marker>
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
  <g class="notes-link" v-for="link in links" :key="link.id">
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";

export default {

  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
  },
  computed: {
    connection() { return this.mindmapSector.connection(150, 50, this.scale)},
    links() { return this.note.links}
  }
}


</script>

<style lang="sass" scoped>
</style>

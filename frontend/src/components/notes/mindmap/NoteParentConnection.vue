<template>
  <g style="stroke:#000000;fill:#000000">
      <marker id="arrowheadParent" markerWidth="8" markerHeight="6" 
      refX="0" refY="3" orient="auto">
        <polygon points="8 0, 0 3, 8 6" style="stroke-width:0"/>
      </marker>
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:2" marker-start="url(#arrowheadParent)"  />
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";

export default {

  props: {
    note: Object,
    mindmap: Object,
    mindmapSector: MindmapSector,
  },
  computed: {
    connection() { return this.mindmap.connectFromParent(this.mindmapSector)}
  }
}


</script>

<style lang="sass" scoped>
</style>

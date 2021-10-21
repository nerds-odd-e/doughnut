<template>
  <g style="stroke:#FF6700;fill:#FF6700">
      <marker id="arrowhead" markerWidth="10" markerHeight="7" 
      refX="8" refY="3" orient="auto">
        <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
      </marker>
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:2" marker-end="url(#arrowhead)"  />
  </g>
</template>

<script>
import NoteTitleWithLink from "../NoteTitleWithLink.vue";
import MindmapSector from "@/models/MindmapSector";

export default {

  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    linkFragment: { type: Object, default: NoteTitleWithLink },
  },
  components: { NoteTitleWithLink },
  computed: {
    connection() { return this.mindmapSector.connection(150, 50, this.scale)}
  }
}


</script>

<style lang="sass" scoped>
</style>

<template>
  <marker id="arrowhead" markerWidth="8" markerHeight="6" 
  refX="8" refY="3" orient="auto">
    <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
  </marker>
  <g style="stroke:#FF6700;fill:#FF6700">
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
  <LinkType v-for="(directAndReverse, linkTypeName, index) in links" :key="linkTypeName"
  v-bind="{linkTypeName, links: directAndReverse.direct, index, mindmapSector, mindmap}"
  />
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import LinksReader from "@/models/LinksReader";
import LinkType from "./LinkType.vue"

export default {

  props: {
    note: Object,
    mindmapSector: MindmapSector,
    mindmap: Object,
  },
  components: { LinkType },
  computed: {
    connection() { return this.mindmap.connectFromParent(this.mindmapSector)},
    links() { return new LinksReader(this.note.links).directLinks },

  },
}


</script>

<style lang="sass" scoped>
</style>

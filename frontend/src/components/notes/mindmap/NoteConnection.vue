<template>
  <marker id="arrowhead" markerWidth="8" markerHeight="6" 
  refX="8" refY="3" orient="auto">
    <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
  </marker>
  <g style="stroke:#FF6700;fill:#FF6700">
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
  <g class="notes-link" v-for="(directAndReverse, linkTypeName) in links" :key="linkTypeName">
    <g class="link-start" >
      <SvgLinkTypeIcon width="30" height="15" :linkTypeName="linkTypeName"/>
    </g>
    <LinkConnection v-for="link in directAndReverse.direct" :key="link.id"
    v-bind="{link, mindmap, mindmapSector }"
    />
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import LinkConnection from "./LinkConnection.vue"
import SvgLinkTypeIcon from "../../svgs/SvgLinkTypeIcon.vue"

export default {

  props: {
    note: Object,
    mindmapSector: MindmapSector,
    mindmap: Object,
  },
  components: { LinkConnection, SvgLinkTypeIcon },
  computed: {
    connection() { return this.mindmap.connectFromParent(this.mindmapSector)},
    links() { return this.note.links},

  },
  methods: {

  }
}


</script>

<style lang="sass" scoped>
</style>

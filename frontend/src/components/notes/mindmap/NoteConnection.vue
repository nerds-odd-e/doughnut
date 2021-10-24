<template>

  <g class="parent-child">
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:15" marker-end="url(#treearrow)"  />
  </g>
  <LinkType v-for="(directAndReverse, linkTypeName, index) in directLinks" :key="linkTypeName"
  v-bind="{linkTypeName, links: directAndReverse.direct, totalLinkTypeCount: directLinkTypeCount, index, mindmapSector, mindmap}"
  />
  <LinkType v-for="(directAndReverse, linkTypeName, index) in reverseLinks" :key="linkTypeName"
  v-bind="{reverse: true, linkTypeName, links: directAndReverse.reverse, totalLinkTypeCount: reverseLinkTypeCount, index, mindmapSector, mindmap}"
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
    directLinks() { return new LinksReader(this.note.links).directLinks },
    reverseLinks() { return new LinksReader(this.note.links).reverseLinks },
    directLinkTypeCount() { return Object.keys(this.directLinks).length },
    reverseLinkTypeCount() { return Object.keys(this.reverseLinks).length },

  },
}


</script>

<style lang="sass" scoped>
</style>

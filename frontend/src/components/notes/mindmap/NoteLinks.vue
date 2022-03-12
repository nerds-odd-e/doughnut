<template>
  <LinkType v-for="(directAndReverse, linkTypeName, index) in directLinks" :key="linkTypeName"
  v-bind="{linkTypeName, links: directAndReverse.direct, totalLinkTypeCount: directLinkTypeCount, index, mindmapSector, mindmap}"
  />
  <LinkType v-for="(directAndReverse, linkTypeName, index) in reverseLinks" :key="linkTypeName"
  v-bind="{reverse: true, linkTypeName, links: directAndReverse.reverse, totalLinkTypeCount: reverseLinkTypeCount, index, mindmapSector, mindmap}"
  />
</template>

<script>
import LinksReader from "@/models/LinksReader";
import LinkType from "./LinkType.vue"

export default {

  props: {
    links: Object,
    mindmapSector: Object,
    mindmap: Object,
  },
  components: { LinkType },
  computed: {
    directLinks() { return new LinksReader(this.links).directLinks },
    reverseLinks() { return new LinksReader(this.links).reverseLinks },
    directLinkTypeCount() { return Object.keys(this.directLinks).length },
    reverseLinkTypeCount() { return Object.keys(this.reverseLinks).length },

  },
}


</script>

<style lang="sass" scoped>
</style>

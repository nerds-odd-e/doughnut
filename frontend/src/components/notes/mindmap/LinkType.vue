<template>
  <g class="notes-link">
    <g class="link-start" :transform="`translate(${linkStart.x}, ${linkStart.y}) rotate(${linkStart.angle * 180 / Math.PI})`">
      <SvgLinkTypeIcon width="30" height="15" :linkTypeName="linkTypeName"/>
    </g>
    <LinkConnection v-for="link in links" :key="link.id"
    v-bind="{link, mindmap, mindmapSector }"
    />
  </g>
</template>

<script>
import LinkConnection from "./LinkConnection.vue"
import SvgLinkTypeIcon from "../../svgs/SvgLinkTypeIcon.vue"

export default {

  props: {
    linkTypeName: String,
    links: Array,
    index: Number,
    mindmapSector: Object,
    mindmap: Object,
  },
  components: { LinkConnection, SvgLinkTypeIcon },
  computed: {
    linkStart() {
       return this.mindmap.outSlot(this.mindmapSector, this.links.length, this.index)
     }

  },
  methods: {
  }
}


</script>

<style lang="sass" scoped>
</style>

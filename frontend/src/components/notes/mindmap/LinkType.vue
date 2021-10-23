<template>
  <g class="notes-links">
    <LinkConnection v-for="link in links" :key="link.id"
    v-bind="{link, mindmap, linkStart }"
    />
    <g class="link-start" :transform="`translate(${linkStart.x}, ${linkStart.y}) rotate(${linkStart.angle * 180 / Math.PI})`">
    <g transform="translate(0, -10)">
      <SvgLinkTypeIcon width="40" height="20" :linkTypeName="linkTypeName"/>
    </g>
    </g>
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

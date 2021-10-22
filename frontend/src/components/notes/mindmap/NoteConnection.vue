<template>
  <marker id="arrowhead" markerWidth="8" markerHeight="6" 
  refX="8" refY="3" orient="auto">
    <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
  </marker>
  <g style="stroke:#FF6700;fill:#FF6700">
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
  <g class="notes-link" v-for="(directAndReverse, linkOfType) in links" :key="linkOfType">
    <LinkConnection v-for="link in directAndReverse.direct" :key="link.id"
    v-bind="{link, mindmap, mindmapSector }"
    />
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import Mindmap from "@/models/Mindmap";
import LinkConnection from "./LinkConnection.vue"

export default {

  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    rootNoteId: [Number, String],
    rootMindmapSector: MindmapSector,
  },
  components: { LinkConnection },
  computed: {
    connection() { return this.mindmap.connectFromParent(this.mindmapSector)},
    links() { return this.note.links},
    mindmap() {
      return new Mindmap(
        this.scale,
        this.rootMindmapSector,
        this.rootNoteId,
        this.$store.getters.getNoteById,
        150,
        50
      )},

  },
  methods: {

  }
}


</script>

<style lang="sass" scoped>
</style>

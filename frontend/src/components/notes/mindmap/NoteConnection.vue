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
    <g style="stroke:green;fill:green" v-for="link in directAndReverse.direct" :key="link.id">
      <line v-bind="linkNoteTo(link.targetNote.id)"
        style="stroke-width:1" marker-end="url(#arrowhead)"  />
    </g>
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";

export default {

  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    rootNoteId: [Number, String],
    rootMindmapSector: MindmapSector,
  },
  computed: {
    connection() { return this.mindmapSector.connection(150, 50, this.scale)},
    links() { return this.note.links},
  },
  methods: {
    linkNoteTo(targetNoteId) {
       const box = this.mindmapSector.linkTo(this.locateNote(targetNoteId), this.scale)
       return box
    },
    locateNote(targetNoteId) {
      const ancestors = this.ancestorsUntilRoot(targetNoteId)
      var sector = this.rootMindmapSector
      for(var i = 0; i < ancestors.length - 1; i++) {
        sector = sector.getChildSector(ancestors[i].childrenIds.length, ancestors[1].childrenIds.indexOf(ancestors[i+1].id))
      }
      return sector
    },
    ancestorsUntilRoot(targetNoteId) {
      const note = this.$store.getters.getNoteById(targetNoteId)
      if(targetNoteId == this.rootNoteId) return [note]
      if (!note.parentId) return null
      return this.ancestorsUntilRoot(note.parentId).concat([note])
    }

  }
}


</script>

<style lang="sass" scoped>
</style>

<template>
  <g style="stroke:green;fill:green" v-if="connection">
    <line v-bind="connection"
      style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";

export default {

  props: {
    link: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    rootNoteId: [Number, String],
    rootMindmapSector: MindmapSector,
  },
  computed: {
    connection() {
      if (!this.targetNoteSector) return

      return this.mindmapSector.linkTo(this.targetNoteSector, this.scale)
    },

    targetNoteSector() {
      const ancestors = this.ancestorsUntilRoot(this.link.targetNote.id)
      if(!ancestors) return
      var sector = this.rootMindmapSector
      for(var i = 0; i < ancestors.length - 1; i++) {
        sector = sector.getChildSector(ancestors[i].childrenIds.length, ancestors[1].childrenIds.indexOf(ancestors[i+1].id))
      }
      return sector
    },

  },
  methods: {
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

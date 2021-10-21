<template>
  <div 
  :class="`note-card ${isHighlighted ? 'highlighted' : ''}`"
  role="card" :aria-label="note.title"
  :style="`top:${coord.y}px; left:${coord.x}px`"
  v-on:click="highlight()">
    <h5 class="note-card-title">
      <component :is="linkFragment" :note="note" class="card-title" />
    </h5>
    <SvgDescriptionIndicator class="description-indicator" v-if="!!note.shortDescription"/>
  </div>
</template>

<script>
import NoteTitleWithMindmapLink from "../NoteTitleWithMindmapLink.vue";
import SvgDescriptionIndicator from "../../svgs/SvgDescriptionIndicator.vue";
import MindmapSector from "@/models/MindmapSector";

export default {
  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    linkFragment: { type: Object, default: NoteTitleWithMindmapLink },
  },
  components: { SvgDescriptionIndicator, NoteTitleWithMindmapLink },
  computed: {
    coord() { return this.mindmapSector.coord(150, 50, this.scale) },
    isHighlighted() { return this.$store.getters.getHighlightNoteId() === this.note.id },

  },
  methods: {
    highlight() { this.$store.commit('highlightNoteId', this.note.id)}
  }

}
</script>

<style lang="sass" scoped>
.note-card
  z-index: 2000
  position: absolute
  width: 150px
  min-height: 50px
  padding: 3px
  background-color: white
  border-width: 1px
  border-style: solid
  border-color: rgb(0,0,0, 0.7)
  border-radius: 10px
.note-card-title
  font-size: 1rem

.highlighted
  z-index: 4000
.highlighted:before
  content: ""
  z-index: -1
  left: -0.5em
  top: -0.1em
  border-width: 2px
  border-style: dotted
  border-color: red
  position: absolute
  border-right-color: transparent
  width: 100%
  height: 100%
  transform: rotate(2deg)
  opacity: 0.7
  border-radius: 50%
  padding: 0.1em 0.25em

.highlighted:after
  content: ""
  z-index: -1
  left: -0.5em
  top: 0.1em
  padding: 0.1em 0.25em
  border-width: 2px
  border-style: dotted
  border-color: red
  border-left-color: transparent
  border-top-color: transparent
  position: absolute
  width: 100%
  height:100%
  transform: rotate(-1deg)
  opacity: 0.7
  border-radius: 50%

.description-indicator
  position: absolute
  left: 5px
  top: 25px

</style>

<template>
  <div 
  :class="`note-card ${isHighlighted ? 'highlighted' : ''}`"
  role="card" :aria-label="note.title"
  :style="`top:${coord.y}px; left:${coord.x}px`"
  v-on:click="highlight()">
    <h5 class="note-card-title">
      <component :is="linkFragment" :note="note" class="card-title" />
    </h5>
    <div class="note-card-body">
      <p>{{ note.shortDescription }}</p>
    </div>
  </div>
</template>

<script>
import NoteTitleWithLink from "../NoteTitleWithLink.vue";
import MindmapSector from "@/models/MindmapSector";

export default {
  props: {
    note: Object,
    scale: Number,
    mindmapSector: MindmapSector,
    linkFragment: { type: Object, default: NoteTitleWithLink },
  },
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
  background-color: white
.note-card-title
  font-size: 1rem
.note-card-body
  font-size: 0.8rem

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

</style>

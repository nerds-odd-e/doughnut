<template>
  <svg class="mindmap-canvas">
    <defs>
      <radialGradient id="myGradient">
        <stop offset="10%" stop-color="white" />
        <stop offset="30%" stop-color="#87CEEB" />
        <stop offset="100%" stop-color="azure" />
      </radialGradient>
    </defs>
    <circle :cx="-offset.x/8" :cy="-offset.y/8" :r="3000 * offset.scale" fill="url('#myGradient')" />
    <marker id="arrowhead" markerWidth="8" markerHeight="6" 
    refX="8" refY="3" orient="auto">
      <polygon points="0 0, 8 3, 0 6" style="stroke-width:0"/>
    </marker>
    <marker id="treearrow" class="parent-child" markerWidth="4" markerHeight="3" 
    refX="3" refY="1.5" orient="auto">
      <polygon points="0 0, 4 1.5, 0 3" style="stroke-width:0"/>
    </marker>


    <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteParentChildConnection'}">
      <template #default="{note, mindmapSector, mindmap}">
        <NoteParentChildConnection v-bind="{
            note,
            mindmapSector,
            mindmap,
            highlighted: highlightNoteId === note.id
          }"
          @highlight="$emit('highlight', noteId)"
        />
      </template>
    </NoteMindmapScaffold>
    <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteLinks'}">
      <template #default="{note, mindmapSector, mindmap}">
        <NoteLinks v-bind="{
            note,
            mindmapSector,
            mindmap,
            highlighted: highlightNoteId === note.id
          }"
          @highlight="$emit('highlight', noteId)"
        />
      </template>
    </NoteMindmapScaffold>
  </svg>
  <NoteMindmapScaffold v-bind="{ highlightNoteId, noteId, mindmap, mindmapSector, noteComponent: 'NoteCard'}" @highlight="highlight">
    <template #default="{note, mindmapSector, mindmap}">
      <NoteCard v-bind="{
          note,
          mindmapSector,
          mindmap,
          highlighted: highlightNoteId === note.id
        }"
        @highlight="$emit('highlight', noteId)"
      />
    </template>
  </NoteMindmapScaffold>
</template>

<script lang="ts">
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import NoteCard from "./NoteCard.vue";
import NoteParentChildConnection from "./NoteParentChildConnection.vue";
import NoteLinks from "./NoteLinks.vue";
import MindmapSector from "@/models/MindmapSector";
import Mindmap from "@/models/Mindmap";

export default {
  name: "NoteMindmap",
  props: {
    highlightNoteId: [String, Number],
    noteId: [String, Number],
    expandChildren: Boolean,
    offset: Object,
  },
  emits: ['highlight'],
  components: {
    NoteMindmapScaffold,
    NoteCard,
    NoteParentChildConnection,
    NoteLinks,
  },
  methods: {
    highlight(id) {this.$emit('highlight', id)}
  },
  computed: {
    mindmapSector() {
      return new MindmapSector(0, 0, this.offset.rotate - Math.PI / 2, Math.PI * 2)
    },
    mindmap() {
      return new Mindmap(
        this.offset.scale,
        this.mindmapSector,
        this.noteId,
        this.$store.getters.getNoteById,
        150,
        50
      )
    },

  },
};
</script>

<style lang="sass">
.mindmap-canvas
  overflow: visible
.parent-child
  stroke: #FFccaa
  fill: #FFccaa

</style>

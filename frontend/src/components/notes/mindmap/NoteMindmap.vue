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


    <NoteMindmapScaffold v-bind="{ noteId, mindmapSector}">
      <template #default="{note, mindmapSector}">
        <NoteParentChildConnection v-bind="{
            note,
            mindmap,
            mindmapSector,
          }"
        />
      </template>
    </NoteMindmapScaffold>
    <NoteMindmapScaffold v-bind="{ noteId, mindmapSector}">
      <template #default="{links, mindmapSector}">
        <NoteLinks v-bind="{
            links,
            mindmap,
            mindmapSector,
          }"
        />
      </template>
    </NoteMindmapScaffold>
  </svg>
  <NoteMindmapScaffold v-bind="{ noteId, mindmapSector}">
    <template #default="{note, mindmapSector}">
      <NoteCard v-bind="{
          note,
          mindmapSector,
          mindmap,
          highlightNoteId,
        }"
        @highlight="$emit('selectNote', $event)"
      />
    </template>
  </NoteMindmapScaffold>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import NoteCard from "./NoteCard.vue";
import NoteParentChildConnection from "./NoteParentChildConnection.vue";
import NoteLinks from "./NoteLinks.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";
import MindmapSector from "../../../models/MindmapSector";
import Mindmap from "../../../models/Mindmap";
import MindmapOffset from "../../../models/MindmapOffset";


export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteMindmap",
  props: {
    highlightNoteId: Number,
    noteId: {type: Number, required: true},
    expandChildren: Boolean,
    offset: {type: Object as PropType<MindmapOffset>, required: true}
  },
  components: {
    NoteMindmapScaffold,
    NoteCard,
    NoteParentChildConnection,
    NoteLinks,
  },
  emits: ['selectNote'],
  computed: {
    mindmapSector() {
      return new MindmapSector(0, 0, this.offset.rotate - Math.PI / 2, Math.PI * 2)
    },
    mindmap() {
      return new Mindmap(
        this.offset.scale,
        this.mindmapSector,
        this.noteId,
        this.piniaStore.getNoteSphereById,
        150,
        50
      )
    },

  },
});
</script>

<style lang="sass">
.mindmap-canvas
  overflow: visible
.parent-child
  stroke: #FFccaa
  fill: #FFccaa

</style>

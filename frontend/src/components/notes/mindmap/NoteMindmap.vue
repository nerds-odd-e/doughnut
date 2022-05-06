<template>
  <svg class="mindmap-canvas" v-if="noteRealms">
    <MindmapSvgDefs />
    <circle
      :cx="-offset.x / 8"
      :cy="-offset.y / 8"
      :r="3000 * offset.scale"
      fill="url('#myGradient')"
    />

    <NoteMindmapScaffold v-bind="{ noteId, noteRealms, mindmapSector }">
      <template #default="{ note, links, mindmapSector }">
        <NoteParentChildConnection
          v-bind="{
            note,
            mindmap,
            mindmapSector,
          }"
        />
        <NoteLinks
          v-bind="{
            links,
            mindmap,
            mindmapSector,
          }"
        />
      </template>
    </NoteMindmapScaffold>
  </svg>

  <NoteMindmapScaffold v-bind="{ noteId, noteRealms, mindmapSector }">
    <template #default="{ note, mindmapSector }">
      <NoteCard
        v-bind="{
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
import MindmapSvgDefs from "./MindmapSvgDefs.vue";
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
    noteId: { type: Number, required: true },
    expandChildren: Boolean,
    offset: { type: Object as PropType<MindmapOffset>, required: true },
  },
  components: {
    NoteMindmapScaffold,
    MindmapSvgDefs,
    NoteCard,
    NoteParentChildConnection,
    NoteLinks,
  },
  emits: ["selectNote"],
  computed: {
    noteRealms() {
      return this.piniaStore.noteRealms;
    },
    mindmapSector() {
      return new MindmapSector(
        0,
        0,
        this.offset.rotate - Math.PI / 2,
        Math.PI * 2
      );
    },
    mindmap() {
      return new Mindmap(this.offset.scale, this.mindmapSector, this.noteId);
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

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
          storageAccessor,
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
import MindmapSector from "../../../models/MindmapSector";
import Mindmap from "../../../models/Mindmap";
import MindmapOffset from "../../../models/MindmapOffset";
import { NoteRealmsReader } from "../../../store/NoteRealmCache";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  name: "NoteMindmap",
  props: {
    noteId: { type: Number, required: true },
    noteRealms: {
      type: Object as PropType<NoteRealmsReader>,
      required: true,
    },
    offset: { type: Object as PropType<MindmapOffset>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
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
    mindmapSector() {
      return new MindmapSector(
        0,
        0,
        this.offset.rotate - Math.PI / 2,
        Math.PI * 2,
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

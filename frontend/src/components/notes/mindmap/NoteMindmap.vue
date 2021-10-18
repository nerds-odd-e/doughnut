<template>
  <NoteMindmapScaffold
    v-bind="{ noteId, mindmapSector, noteComponent: 'NoteCard'}"
  />
</template>

<script lang="ts">
import NoteMindmapScaffold from "./NoteMindmapScaffold.vue";
import MindmapSector from "@/models/MindmapSector";

export default {
  name: "NoteMindmap",
  props: {
    noteId: Number,
  },
  emits: ["updated"],
  components: { NoteMindmapScaffold },
  computed: {
    mindmapSector() {
      return new MindmapSector(0, 0, 0, Math.PI * 2)
    },
  },
};
</script>

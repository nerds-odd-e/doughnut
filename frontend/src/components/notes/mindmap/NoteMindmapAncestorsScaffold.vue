<template>
  <template v-if="ancestors.length > 0">
  <component v-bind:is="noteComponent" v-bind="{ note: noteViewedByUser, scale, mindmapSector: mindmapSector.getChildSector(1, 0) }"/>
  <NoteMindmapAncestorsScaffold
    v-bind="{ noteComponent, scale, ancestors: ancestors.slice(0, ancestors.length - 1), mindmapSector: mindmapSector.getChildSector(1, 0) }"
  />
  </template>
</template>

<script lang="ts">
import MindmapSector from "@/models/MindmapSector";
import NoteCard from "./NoteCard.vue";
import NoteParentConnection from "./NoteParentConnection.vue";

export default {
  name: "NoteMindmap",
  props: {
    ancestors: Array,
    scale: Number,
    mindmapSector: MindmapSector,
    noteComponent: String,
  },
  components: { NoteCard, NoteParentConnection },
  computed: {
    noteViewedByUser() {
      return this.ancestors[this.ancestors.length - 1]
    },
  },
};
</script>

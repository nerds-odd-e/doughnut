<template>
  <template v-if="note">
    <slot v-bind="{ note, links, mindmapSector }"/>
    <NoteMindmapScaffold
      v-for="(childId, index) in childrenIds"
      v-bind="{
        noteId: childId,
        mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
      }"
      :key="childId"
    >
      <template #default="{note, links, mindmapSector}">
        <slot v-bind="{ note, links, mindmapSector }"/>
      </template>
    </NoteMindmapScaffold>
  </template>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import MindmapSector from "../../../models/MindmapSector";

import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteMindmap",
  props: {
    noteId: { type: Number, required: true},
    mindmapSector: MindmapSector,
  },
  computed: {
    note() {
      return this.piniaStore.getNoteById(this.noteId);
    },
    links() {
      return this.piniaStore.getLinksById(this.noteId);
    },
    childrenIds() {
      return this.piniaStore.getChildrenIdsByParentId(this.noteId);
    },
  },
});
</script>

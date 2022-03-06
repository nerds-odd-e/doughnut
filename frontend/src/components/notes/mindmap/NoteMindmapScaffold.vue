<template>
  <template v-if="note">
    <slot v-bind="{ note, mindmapSector }"/>
    <NoteMindmapScaffold
      v-for="(childId, index) in childrenIds"
      v-bind="{
        noteId: childId,
        mindmapSector: mindmapSector.getChildSector(childrenIds.length, index),
      }"
      :key="childId"
    >
      <template #default="{note, mindmapSector}">
        <slot v-bind="{ note, mindmapSector }"/>
      </template>
    </NoteMindmapScaffold>
  </template>
</template>

<script>
import MindmapSector from "@/models/MindmapSector";
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  name: "NoteMindmap",
  props: {
    noteId: [String, Number],
    mindmapSector: MindmapSector,
  },
  computed: {
    note() {
      return this.store.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.store.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

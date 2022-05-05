<template>
  <template v-if="note">
    <slot v-bind="{ note, links, mindmapSector }"/>
    <NoteMindmapScaffold
      v-for="(child, index) in children"
      v-bind="{
        noteId: child.id,
        mindmapSector: mindmapSector.getChildSector(children.length, index),
      }"
      :key="child.id"
    >
      <template #default="{note, links, mindmapSector}">
        <slot v-bind="{ note, links, mindmapSector }"/>
      </template>
    </NoteMindmapScaffold>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import MindmapSector from "../../../models/MindmapSector";

import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteMindmap",
  props: {
    noteId: { type: Number, required: true},
    mindmapSector: Object as PropType<MindmapSector>,
  },
  computed: {
    noteRealm() {
      return this.piniaStore.getNoteRealmById(this.noteId);
    },
    note() {
      return this.noteRealm?.note
    },
    links() {
      return this.noteRealm?.links
    },
    children() {
      return this.noteRealm?.children
    },
  },
});
</script>

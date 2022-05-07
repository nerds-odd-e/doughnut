<template>
  <template v-if="note">
    <slot v-bind="{ note, links, mindmapSector }" />
    <NoteMindmapScaffold
      v-for="(child, index) in children"
      v-bind="{
        noteId: child.id,
        noteRealms,
        mindmapSector: mindmapSector.getChildSector(children.length, index),
      }"
      :key="child.id"
    >
      <template #default="{ note, links, mindmapSector }">
        <slot v-bind="{ note, links, mindmapSector }" />
      </template>
    </NoteMindmapScaffold>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import MindmapSector from "../../../models/MindmapSector";
import { NoteRealmsReader } from "../../../store/NoteRealmCache";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    mindmapSector: { type: Object as PropType<MindmapSector>, required: true },
    noteRealms: {
      type: Object as PropType<NoteRealmsReader>,
      required: true,
    },
  },
  computed: {
    noteRealm() {
      return this.noteRealms.getNoteRealmById(this.noteId);
    },
    note() {
      return this.noteRealm?.note;
    },
    links() {
      return this.noteRealm?.links;
    },
    children() {
      return this.noteRealm?.children;
    },
  },
});
</script>

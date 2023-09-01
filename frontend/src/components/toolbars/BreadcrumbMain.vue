<template>
  <Breadcrumb
    v-bind="{
      ancestors: notePosition?.ancestors,
      notebook: notePosition?.notebook,
      circle: storageAccessor.circle,
    }"
  >
    <NoteNewButton
      v-if="parentId && !readonly"
      v-bind="{ parentId, storageAccessor }"
      button-title="Add Sibling Note"
    >
      <SvgAddSibling />
    </NoteNewButton>
  </Breadcrumb>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewButton from "./NoteNewButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import Breadcrumb from "./Breadcrumb.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    readonly: { type: Boolean },
  },
  components: {
    NoteNewButton,
    Breadcrumb,
    SvgAddSibling,
  },
  computed: {
    parentId() {
      if (!this.notePosition) return undefined;
      if (!this.storageAccessor.selectedNote) return undefined;
      const { ancestors } = this.notePosition;
      if (ancestors.length > 0) {
        return ancestors[ancestors.length - 1]?.id;
      }
      return undefined;
    },
    notePosition() {
      return this.storageAccessor.notePosition;
    },
  },
});
</script>

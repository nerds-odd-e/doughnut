<template>
  <Breadcrumb v-bind="selectedNotePosition">
    <NoteNewButton
      v-bind="{ parentId, historyWriter }"
      button-title="Add Sibling Note"
      v-if="parentId"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    >
      <SvgAddSibling />
    </NoteNewButton>
  </Breadcrumb>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Breadcrumb from "./Breadcrumb.vue";
import { ViewTypeName } from "../../models/viewTypes";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import NoteNewButton from "./NoteNewButton.vue";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    selectedNoteId: { type: Number, required: true },
    selectedNotePosition: {
      type: Object as PropType<Generated.NotePositionViewedByUser>,
      required: true,
    },
    viewType: { type: String as PropType<ViewTypeName>, required: true },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
  },
  emits: ["noteRealmUpdated"],
  components: {
    Breadcrumb,
    SvgAddSibling,
    NoteNewButton,
  },
  computed: {
    parentId() {
      const { ancestors } = this.selectedNotePosition;
      if (ancestors.length > 0) {
        return ancestors[ancestors.length - 1].id;
      }
      return undefined;
    },
  },
});
</script>

<template>
  <Breadcrumb v-bind="selectedNotePosition" />
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <NoteNewButton
        :parent-id="parentId"
        button-title="Add Sibling Note"
        v-if="parentId"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      >
        <SvgAddSibling />
      </NoteNewButton>
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Breadcrumb from "./Breadcrumb.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import { ViewTypeName } from "../../models/viewTypes";
import ToolbarFrame from "./ToolbarFrame.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import NoteNewButton from "./NoteNewButton.vue";
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  props: {
    selectedNoteId: { type: Number, required: true },
    selectedNotePosition: {
      type: Object as PropType<Generated.NotePositionViewedByUser>,
      required: true,
    },
    viewType: { type: String as PropType<ViewTypeName>, required: true },
  },
  emits: ["noteRealmUpdated"],
  components: {
    Breadcrumb,
    ToolbarFrame,
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

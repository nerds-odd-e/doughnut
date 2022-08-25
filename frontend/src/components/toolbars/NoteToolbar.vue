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

      <div class="dropdown">
        <button
          class="btn btn-light dropdown-toggle"
          id="dropdownMenuButton"
          data-bs-toggle="dropdown"
          aria-haspopup="true"
          aria-expanded="false"
          role="button"
          title="more options"
        >
          <SvgCog />
        </button>
        <div class="dropdown-menu dropdown-menu-end">
          <NoteDeleteButton
            class="dropdown-item"
            :note-id="selectedNoteId"
            @note-deleted="$emit('noteDeleted', $event)"
          />
        </div>
      </div>
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
import SvgCog from "../svgs/SvgCog.vue";
import NoteNewButton from "./NoteNewButton.vue";
import usePopups from "../commons/Popups/usePopup";
import NoteDeleteButton from "./NoteDeleteButton.vue";

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
  emits: ["noteDeleted", "noteRealmUpdated"],
  components: {
    Breadcrumb,
    ToolbarFrame,
    SvgCog,
    SvgAddSibling,
    NoteNewButton,
    NoteDeleteButton,
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

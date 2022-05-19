<template>
  <nav class="navbar toolbar">
    <NoteButtons
      :note="selectedNote"
      :view-type="viewType"
      :feature-toggle="featureToggle"
      @note-deleted="$emit('noteDeleted', $event)"
    />
    <div class="btn-group btn-group-sm">
      <PopupButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <template #dialog_body="{ doneHandler }">
          <LinkNoteDialog
            :note="selectedNote"
            @done="
              doneHandler($event);
              $emit('noteRealmUpdated', $event);
            "
          />
        </template>
      </PopupButton>
      <NoteUndoButton @note-realm-updated="$emit('noteRealmUpdated', $event)" />
    </div>
  </nav>
  <Breadcrumb v-bind="selectedNotePosition" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Breadcrumb from "./Breadcrumb.vue";
import NoteButtons from "./NoteButtons.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import { ViewTypeName } from "../../models/viewTypes";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    selectedNotePosition: {
      type: Object as PropType<Generated.NotePositionViewedByUser>,
      required: true,
    },
    viewType: { type: String as PropType<ViewTypeName>, required: true },
  },
  emits: ["noteDeleted", "noteRealmUpdated"],
  components: {
    NoteButtons,
    NoteUndoButton,
    Breadcrumb,
    PopupButton,
    SvgSearch,
    LinkNoteDialog,
  },
  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
});
</script>

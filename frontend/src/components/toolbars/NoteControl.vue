<template>
  <nav class="navbar toolbar">
    <NoteButtons
      v-if="selectedNote"
      :note="selectedNote"
      :viewType="viewType"
      :featureToggle="featureToggle"
    />
    <div class="btn-group btn-group-sm">
      <PopupButton title="link note">
        <template #face>
          <SvgSearch />
        </template>
        <template #default="{doneHandler}">
          <LinkNoteDialog :note="selectedNote" @done="doneHandler($event)" />
        </template>
      </PopupButton>
      <NoteUndoButton />
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

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    selectedNote: Object as PropType<Generated.Note>,
    selectedNotePosition: Object as PropType<Generated.NotePositionViewedByUser>,
    viewType: String,
  },
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

<template>
  <ToolbarFrame>
    <NoteButtons
      :note="selectedNote"
      :view-type="viewType"
      :feature-toggle="featureToggle"
      @note-deleted="$emit('noteDeleted', $event)"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
      @new-note-added="$emit('newNoteAdded', $event)"
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
      <PopupButton title="associate wikidata">
        <template #button_face>
          <SvgWikiData />
        </template>
        <template #dialog_body="{ doneHandler }">
          <WikidataAssociationDialog
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
  </ToolbarFrame>
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
import ToolbarFrame from "./ToolbarFrame.vue";
import SvgWikiData from "../svgs/SvgWikiData.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";

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
  emits: ["noteDeleted", "noteRealmUpdated", "newNoteAdded"],
  components: {
    NoteButtons,
    NoteUndoButton,
    Breadcrumb,
    PopupButton,
    SvgSearch,
    LinkNoteDialog,
    ToolbarFrame,
    SvgWikiData,
    WikidataAssociationDialog,
  },
  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
});
</script>

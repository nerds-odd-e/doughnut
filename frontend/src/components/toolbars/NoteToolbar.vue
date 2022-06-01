<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <ViewTypeButtons v-bind="{ viewType, noteId: selectedNote.id }" />

      <NoteNewButton
        :parent-id="selectedNote.id"
        button-title="Add Child Note"
        @new-note-added="onNewNoteAdded($event)"
      >
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        :parent-id="selectedNote.parentId"
        button-title="Add Sibling Note"
        v-if="!!selectedNote.parentId"
        @new-note-added="onNewNoteAdded($event)"
      >
        <SvgAddSibling />
      </NoteNewButton>

      <PopupButton title="edit note">
        <template #button_face>
          <SvgEdit />
        </template>
        <template #dialog_body="{ doneHandler }">
          <NoteEditDialog
            :note-id="selectedNote.id"
            @done="
              doneHandler($event);
              $emit('noteRealmUpdated');
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
      <a
        class="btn btn-light dropdown-toggle"
        data-bs-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
        role="button"
        title="more options"
      >
        <SvgCog />
      </a>
      <div class="dropdown-menu dropdown-menu-right">
        <PopupButton title="Edit review settings">
          <template #button_face>
            <SvgReviewSetting />Edit review settings
          </template>
          <template #dialog_body="{ doneHandler }">
            <ReviewSettingEditDialog
              :note-id="selectedNote.id"
              :title="selectedNote.title"
              @done="doneHandler($event)"
            />
          </template>
        </PopupButton>
        <button class="dropdown-item" title="Delete note" @click="deleteNote">
          <SvgRemove />Delete note
        </button>

        <PopupButton title="Add comment">
          <template #button_face> Add comment </template>
          <template #dialog_body="{ doneHandler }">
            <CommentCreateDialog
              :note-id="selectedNote.id"
              v-if="featureToggle"
              @done="
                doneHandler($event);
                $emit('noteRealmUpdated');
              "
            />
          </template>
        </PopupButton>
      </div>
    </div>
  </ToolbarFrame>
  <Breadcrumb v-bind="selectedNotePosition" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Breadcrumb from "./Breadcrumb.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import { ViewTypeName } from "../../models/viewTypes";
import ToolbarFrame from "./ToolbarFrame.vue";
import SvgWikiData from "../svgs/SvgWikiData.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import NoteNewButton from "./NoteNewButton.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import ReviewSettingEditDialog from "../review/ReviewSettingEditDialog.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import CommentCreateDialog from "../notes/CommentCreateDialog.vue";

import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
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
    NoteUndoButton,
    Breadcrumb,
    PopupButton,
    SvgSearch,
    LinkNoteDialog,
    ToolbarFrame,
    SvgWikiData,
    WikidataAssociationDialog,
    SvgCog,
    SvgAddChild,
    SvgAddSibling,
    SvgRemove,
    NoteNewButton,
    ViewTypeButtons,
    SvgReviewSetting,
    ReviewSettingEditDialog,
    SvgEdit,
    NoteEditDialog,
    CommentCreateDialog,
  },
  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
  methods: {
    onNewNoteAdded(newNote: Generated.NoteRealmWithPosition) {
      this.$emit("newNoteAdded", newNote);
    },
    async deleteNote() {
      if (await this.popups.confirm(`Confirm to delete this note?`)) {
        const { id, parentId } = this.selectedNote;
        await this.storedApi.deleteNote(id);
        if (parentId) {
          this.$emit("noteDeleted", id);
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
});
</script>

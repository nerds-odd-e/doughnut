<template>
  <div class="btn-toolbar" :key="note.id">
    <ViewTypeButtons v-bind="{ viewType, noteId: note.id }" />

    <div class="btn-group btn-group-sm">
      <NoteNewButton :parent-id="note.id" button-title="Add Child Note">
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        :parent-id="note.parentId"
        button-title="Add Sibling Note"
        v-if="!!note.parentId"
      >
        <SvgAddSibling />
      </NoteNewButton>

      <PopupButton title="edit note">
        <template #button_face>
          <SvgEdit />
        </template>
        <template #dialog_body="{ doneHandler }">
          <NoteEditDialog :note-id="note.id" @done="doneHandler($event)" />
        </template>
      </PopupButton>

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
              :note-id="note.id"
              :title="note.title"
              @done="doneHandler($event)"
            />
          </template>
        </PopupButton>
        <button class="dropdown-item" title="Delete note" @click="deleteNote">
          <SvgRemove />Delete note
        </button>

        <PopupButton title="Add comment">
          <template #button_face> Add comment </template>
          <template #dialog_body>
            <CommentCreateDialog :note-id="note.id" v-if="featureToggle" />
          </template>
        </PopupButton>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import NoteNewButton from "./NoteNewButton.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { ViewTypeName } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import usePopups from "../commons/Popups/usePopup";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import ReviewSettingEditDialog from "../review/ReviewSettingEditDialog.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import CommentCreateDialog from "../notes/CommentCreateDialog.vue";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  props: {
    note: Object,
    viewType: {
      type: String as PropType<ViewTypeName>,
      default: () => undefined,
    },
    featureToggle: Boolean,
  },
  emits: ["noteDeleted"],
  components: {
    SvgCog,
    SvgAddChild,
    SvgAddSibling,
    SvgRemove,
    NoteNewButton,
    ViewTypeButtons,
    PopupButton,
    SvgReviewSetting,
    ReviewSettingEditDialog,
    SvgEdit,
    NoteEditDialog,
    CommentCreateDialog,
  },
  methods: {
    async deleteNote() {
      if (await this.popups.confirm(`Confirm to delete this note?`)) {
        const { id, parentId } = this.note;
        await this.storedApi.deleteNote(id);
        if (parentId) {
          if (this.viewType === "cards") {
            this.$router.push({
              name: "noteShow",
              params: { rawNoteId: parentId, viewType: this.viewType },
            });
            return;
          }
          this.$emit("noteDeleted", id);
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
});
</script>

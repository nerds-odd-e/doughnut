<template>
  <div class="btn-toolbar" :key="note.id">
    <ViewTypeButtons v-bind="{ viewType, noteId: note.id }" />

    <div class="btn-group btn-group-sm">
      <NoteNewButton :parentId="note.id" buttonTitle="Add Child Note">
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        :parentId="note.parentId"
        buttonTitle="Add Sibling Note"
        v-if="!!note.parentId"
      >
        <SvgAddSibling />
      </NoteNewButton>

      <PopupButton title="edit note">
        <template v-slot:face>
          <SvgEdit/>
        </template>
        <template #default="{doneHandler}">
          <NoteEditDialog :noteId="note.id" @done="doneHandler($event)"/>
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
          <template v-slot:face>
            <SvgReviewSetting />Edit review settings
          </template>
          <template #default="{ doneHandler }">
            <ReviewSettingEditDialog
              :noteId="note.id"
              :title="note.title"
              @done="doneHandler($event)"
            />
          </template>
        </PopupButton>
        <button class="dropdown-item" title="Delete note" v-on:click="deleteNote">
          <SvgRemove />Delete note
        </button>
      </div>
      <NoteDownloadButton :note="note" v-if="featureToggle" />
    </div>
  </div>
</template>

<script>
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import NoteDownloadButton from "./NoteDownloadButton.vue"
import NoteNewButton from "./NoteNewButton.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { viewType } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import usePopups from "../commons/Popups/usePopup";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue";
import ReviewSettingEditDialog from "../review/ReviewSettingEditDialog.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";

export default ({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  name: "NoteButtons",
  props: {
    note: Object,
    viewType: String,
    featureToggle: Boolean,
  },
  components: {
    SvgCog,
    SvgAddChild,
    SvgAddSibling,
    SvgRemove,
    NoteNewButton,
    ViewTypeButtons,
    NoteDownloadButton,
    PopupButton,
    SvgReviewSetting,
    ReviewSettingEditDialog,
    SvgEdit,
    NoteEditDialog
  },
  methods: {
    async deleteNote() {
      if (await this.popups.confirm(`Are you sure to delete this note?`)) {
        const parentId = this.note.parentId;
        await this.storedApi.deleteNote(this.note.id);
        this.$emit("ensureVisible", parentId);
        if (parentId) {
          if (viewType(this.viewType).redirectAfterDelete) {
            this.$router.push({
              name: "noteShow",
              params: { rawNoteId: parentId, viewType: this.viewType },
            });
          }
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
});
</script>

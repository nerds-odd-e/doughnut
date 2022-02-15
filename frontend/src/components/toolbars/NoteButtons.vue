<template>
  <div class="btn-toolbar" :key="note.id">
    <ViewTypeButtons v-bind="{ viewType, noteId: note.id }" />

    <div class="btn-group btn-group-sm">
      <NoteNewButton
        :parentId="note.id"
        buttonTitle="Add Child Note"
       >
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        :parentId="note.parentId"
        buttonTitle="Add Sibling Note"
        v-if="!!note.parentId">
          <SvgAddSibling />
      </NoteNewButton>

      <NoteEditButton :noteId="note.id" :oldTitle="note.title" />

      <LinkNoteButton :note="note" />

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
        <ReviewSettingEditButton :noteId="note.id" :oldTitle="note.title">
          Edit review settings
        </ReviewSettingEditButton>
        <NoteSplitButton :noteId="note.id" :oldTitle="note.title">
          Split this note
        </NoteSplitButton>
        <button
          class="dropdown-item"
          title="delete note"
          v-on:click="deleteNote"
        >
          <SvgRemove />
          Delete
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
import LinkNoteButton from "../links/LinkNoteButton.vue";
import ReviewSettingEditButton from "../review/ReviewSettingEditButton.vue";
import NoteEditButton from "./NoteEditButton.vue";
import NoteSplitButton from "./NoteSplitButton.vue";
import NoteDownloadButton from "./NoteDownloadButton.vue"
import NoteNewButton from "./NoteNewButton.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { storedApiDeleteNote } from "../../storedApi";
import { viewType } from "../../models/viewTypes";
export default {
  name: "NoteButtons",
  props: {
    note: Object,
    viewType: String,
  },
  components: {
    SvgCog,
    SvgAddChild,
    SvgAddSibling,
    ReviewSettingEditButton,
    SvgRemove,
    LinkNoteButton,
    NoteEditButton,
    NoteSplitButton,
    NoteNewButton,
    ViewTypeButtons,
    NoteDownloadButton
  },
  computed: {
    featureToggle() {
      return this.$store.getters.getFeatureToggle();
    },
  },
  methods: {
    async deleteNote() {
      if (await this.$popups.confirm(`Are you sure to delete this note?`)) {
        const parentId = this.note.parentId;
        await storedApiDeleteNote(this.$store, this.note.id);
        this.$emit("ensureVisible", parentId);
        if (parentId) {
          if (viewType(this.viewType).redirectAfterDelete) {
            this.$router.push({
              name: "noteShow",
              params: { noteId: parentId, viewType: this.viewType },
            });
          }
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
};
</script>

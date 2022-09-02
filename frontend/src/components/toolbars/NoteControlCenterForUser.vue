<template>
  <div class="btn-group btn-group-sm">
    <template v-if="!selectedNote">
      <PopupButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <template #dialog_body="{ doneHandler }">
          <LinkNoteDialog
            v-bind="{ storageAccessor }"
            @done="doneHandler($event)"
          />
        </template>
      </PopupButton>
    </template>
    <template v-if="selectedNote">
      <ViewTypeButtons v-bind="{ viewType, noteId: selectedNote.id }" />
      <NoteNewButton
        v-bind="{ parentId: selectedNote.id, storageAccessor }"
        button-title="Add Child Note"
      >
        <SvgAddChild />
      </NoteNewButton>

      <PopupButton title="edit note">
        <template #button_face>
          <SvgEdit />
        </template>
        <template #dialog_body="{ doneHandler }">
          <NoteEditDialog
            v-bind="{ note: selectedNote, storageAccessor }"
            @done="doneHandler($event)"
          />
        </template>
      </PopupButton>

      <PopupButton title="associate wikidata">
        <template #button_face>
          <SvgWikiData />
        </template>
        <template #dialog_body="{ doneHandler }">
          <WikidataAssociationDialog
            v-bind="{ note: selectedNote, storageAccessor }"
            @done="doneHandler($event)"
          />
        </template>
      </PopupButton>

      <PopupButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <template #dialog_body="{ doneHandler }">
          <LinkNoteDialog
            v-bind="{ note: selectedNote, storageAccessor }"
            @done="doneHandler($event)"
          />
        </template>
      </PopupButton>
      <div class="dropdown">
        <button
          class="btn dropdown-toggle"
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
            v-bind="{ noteId: selectedNote.id, storageAccessor }"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import SvgWikiData from "../svgs/SvgWikiData.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { sanitizeViewTypeName } from "../../models/viewTypes";
import SvgCog from "../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
  components: {
    NoteNewButton,
    SvgAddChild,
    SvgEdit,
    NoteEditDialog,
    SvgWikiData,
    WikidataAssociationDialog,
    SvgSearch,
    LinkNoteDialog,
    ViewTypeButtons,
    SvgCog,
    NoteDeleteButton,
    PopupButton,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
    viewType() {
      return sanitizeViewTypeName(this.$route.meta.viewType as string);
    },
  },
});
</script>

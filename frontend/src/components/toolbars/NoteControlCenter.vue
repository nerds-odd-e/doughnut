<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <template v-if="!selectedNoteId">
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
      <template v-if="selectedNoteId">
        <ViewTypeButtons v-bind="{ viewType, noteId: selectedNoteId }" />
        <NoteNewButton
          v-bind="{ parentId: selectedNoteId, storageAccessor }"
          button-title="Add Child Note"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
        >
          <SvgAddChild />
        </NoteNewButton>

        <PopupButton title="edit note">
          <template #button_face>
            <SvgEdit />
          </template>
          <template #dialog_body="{ doneHandler }">
            <NoteDialogFrame :note-id="selectedNoteId">
              <template #default="{ note }">
                <NoteEditDialog
                  v-bind="{ note, storageAccessor }"
                  @done="doneHandler($event)"
                />
              </template>
            </NoteDialogFrame>
          </template>
        </PopupButton>

        <PopupButton title="associate wikidata">
          <template #button_face>
            <SvgWikiData />
          </template>
          <template #dialog_body="{ doneHandler }">
            <NoteDialogFrame :note-id="selectedNoteId">
              <template #default="{ note }">
                <WikidataAssociationDialog
                  v-bind="{ note, storageAccessor }"
                  @done="doneHandler($event)"
                />
              </template>
            </NoteDialogFrame>
          </template>
        </PopupButton>

        <PopupButton title="link note">
          <template #button_face>
            <SvgSearch />
          </template>
          <template #dialog_body="{ doneHandler }">
            <NoteDialogFrame :note-id="selectedNoteId">
              <template #default="{ note }">
                <LinkNoteDialog
                  v-bind="{ note, storageAccessor }"
                  @done="
                    doneHandler($event);
                    $emit('noteRealmUpdated', $event);
                  "
                />
              </template>
            </NoteDialogFrame>
          </template>
        </PopupButton>
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
              v-bind="{ noteId: selectedNoteId, storageAccessor }"
              @note-deleted="$emit('noteDeleted', $event)"
            />
          </div>
        </div>
      </template>

      <NoteUndoButton v-bind="{ storageAccessor }" />
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteDialogFrame from "../notes/NoteDialogFrame.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import SvgWikiData from "../svgs/SvgWikiData.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { ViewTypeName } from "../../models/viewTypes";
import SvgCog from "../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: {
    selectedNoteId: Number,
    viewType: { type: String as PropType<ViewTypeName> },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["noteDeleted", "noteRealmUpdated"],
  components: {
    ToolbarFrame,
    NoteUndoButton,
    NoteNewButton,
    SvgAddChild,
    SvgEdit,
    NoteDialogFrame,
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
});
</script>

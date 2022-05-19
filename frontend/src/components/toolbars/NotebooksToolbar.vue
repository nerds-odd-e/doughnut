<template>
  <nav class="navbar toolbar">
    <div class="btn-group btn-group-sm">
      <PopupButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <template #dialog_body="{ doneHandler }">
          <LinkNoteDialog @done="doneHandler($event)" />
        </template>
      </PopupButton>
      <NoteUndoButton @note-realm-updated="$emit('noteRealmUpdated', $event)" />
    </div>
  </nav>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";

export default defineComponent({
  emits: ["noteRealmUpdated"],
  components: {
    NoteUndoButton,
    PopupButton,
    SvgSearch,
    LinkNoteDialog,
  },
});
</script>

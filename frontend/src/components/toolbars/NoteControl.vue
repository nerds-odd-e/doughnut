<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    />
    <div class="btn-group btn-group-sm">
      <LinkNoteButton :note="currentNote" />
      <NoteUndoButton/>
    </div>
  </nav>
  <slot />
</template>

<script>

import NoteButtons from './NoteButtons.vue'
import NoteUndoButton from "./NoteUndoButton.vue";
import LinkNoteButton from "../links/LinkNoteButton.vue";

export default {
  components: {
    NoteButtons,
    NoteUndoButton,
    LinkNoteButton,
  },
  computed: {
    currentNote() { return this.$store.getters.getHighlightNote()},
    viewType() { return this.$store.getters.getViewType()},
  },
};
</script>

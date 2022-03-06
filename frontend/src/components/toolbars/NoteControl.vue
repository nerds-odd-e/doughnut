<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    :featureToggle="featureToggle"
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
import storedComponent from '../../store/storedComponent';

export default storedComponent({
  components: {
    NoteButtons,
    NoteUndoButton,
    LinkNoteButton,
  },
  computed: {
    currentNote() { return this.piniaStore.getHighlightNote() },
    viewType() { return this.piniaStore.viewType },
    featureToggle() { return this.piniaStore.featureToggle },
  },
});
</script>

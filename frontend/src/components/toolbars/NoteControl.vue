<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    />
    <div class="btn-group btn-group-sm">
      <NoteUndoButton/>
    </div>
  </nav>
  <slot />
</template>

<script>

import NoteButtons from './NoteButtons.vue'
import NoteUndoButton from "./NoteUndoButton.vue";
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  components: { NoteButtons, NoteUndoButton },
  computed: {
    currentNote() { return this.store.getHighlightNote() },
    viewType() { return this.store.getViewType() }
  },
};
</script>

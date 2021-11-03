<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    />
  </nav>
  <slot />
</template>

<script>

import NoteButtons from './NoteButtons.vue'

export default {
  props: {
    noteId: [String, Number],
    viewType: String,
  },
  components: { NoteButtons },
  computed: {
    currentNote() { return this.$store.getters.getNoteById(this.noteId)},
  },
};
</script>

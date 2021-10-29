<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote" :note="currentNote"/>
  </nav>
  <slot />
</template>

<script>

import NoteButtons from '../notes/NoteButtons.vue'

export default {
  props: { noteId: [String, Number] },
  components: { NoteButtons },
  computed: {
    currentNote() { return this.$store.getters.getNoteById(this.noteId)},
  },
  mounted() {
    this.$store.commit('highlightNoteId', this.noteId)
  },
  unmounted() {
    this.$store.commit('highlightNoteId', null)
  }
};
</script>

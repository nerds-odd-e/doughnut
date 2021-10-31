<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote" :note="currentNote" :deleteRedirect="deleteRedirect"/>
  </nav>
  <slot />
</template>

<script>

import NoteButtons from '../notes/NoteButtons.vue'

export default {
  props: {
    noteId: [String, Number],
    deleteRedirect: {type: Boolean, required: true}
  },
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

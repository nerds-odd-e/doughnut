<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :deleteRedirect="deleteRedirect"
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
    deleteRedirect: {type: Boolean, required: true},
    viewType: String,
  },
  components: { NoteButtons },
  computed: {
    currentNote() { return this.$store.getters.getNoteById(this.noteId)},
  },
};
</script>

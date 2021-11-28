<template>
  <nav class="navbar toolbar">
    <NoteButtons v-if="currentNote"
    :note="currentNote"
    :viewType="viewType"
    @updateLanguage="$emit('updateLanguage', $event)"
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
  emits: ['updateLanguage'],
  components: { NoteButtons },
  computed: {
    currentNote() { return this.$store.getters.getNoteById(this.noteId)},
  },
};
</script>

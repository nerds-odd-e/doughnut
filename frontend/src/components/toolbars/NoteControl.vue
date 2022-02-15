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
  emits: ['updateLanguage'],
  components: { NoteButtons },
  computed: {
    currentNote() { return this.$store.getters.getHighlightNote()},
    viewType() { return this.$store.getters.getViewType()},
  },
};
</script>

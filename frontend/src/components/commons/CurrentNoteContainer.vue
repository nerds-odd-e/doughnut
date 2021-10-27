<template>
  <slot />
</template>

<script>

export default {
  props: { noteId: [String, Number] },
  mounted() {
    this.$store.commit('highlightNoteId', this.noteId)
  },
  unmounted() {
    this.$store.commit('highlightNoteId', null)
  }
};
</script>

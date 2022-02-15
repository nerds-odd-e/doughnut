<template>
  <div class="container">
    <NoteOverview
      v-bind="{ noteId, expandChildren, language }"
      :key="noteId"
    />
  </div>
</template>

<script lang="ts">
import NoteOverview from "../NoteOverview.vue";

export default {
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
    language: String,
  },
  components: { NoteOverview },
  computed: {
  },
};
</script>

<style lang="sass" scoped>

:deep(.note-body)
  border-width: 0px
  border-left-width: 3px

</style>

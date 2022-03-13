<template>
  <div class="container">
    <NoteOverview
      v-bind="{ noteId, expandChildren }"
      :key="noteId"
    />
  </div>
</template>

<script lang="ts">

import NoteOverview from "../NoteOverview.vue";

export default {
  props: {
    noteId: Number,
    expandChildren: { type: Boolean, required: true },
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

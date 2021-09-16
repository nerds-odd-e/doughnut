<template>
  <NoteShow
    v-bind="{ ...noteViewedByUser, ancestors, notebook, level: 1 }"
    @updated="$emit('updated')"
  />
  <p
    data-testid="overview-note-title"
    class="overview-note-title"
    v-for="child in children"
    :key="child.noteId"
  >
    {{ child.title }}
  </p>
</template>

<script lang="ts">
import NoteShow from "./NoteShow.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: Number,
    children: Array,
    ancestors: Array,
    notebook: Object,
    recentlyUpdated: Boolean,
  },
  emits: ["updated"],
  components: { NoteShow },
  computed: {
    noteViewedByUser() { return this.$store.getters.getNoteById(this.noteId)}
  }
};
</script>

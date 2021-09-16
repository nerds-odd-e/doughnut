<template>
  <NoteShow
    v-bind="{ ...noteViewedByUser, level: 1 }"
    @updated="$emit('updated')"
  />
  <NoteShow
    v-for="child in childrenx"
    v-bind="{ ...child, level: 2 }"
    :key="child.note.id"
  />
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
    noteViewedByUser() { return this.$store.getters.getNoteById(this.noteId)},
    childrenx() { return this.$store.getters.getChildrenOfParentId(this.noteId)}
  }
};
</script>

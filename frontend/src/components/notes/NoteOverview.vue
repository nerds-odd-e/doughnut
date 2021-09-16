<template>
  <NoteShow
    v-bind="{ ...noteViewedByUser, level: 1 }"
    @updated="$emit('updated')"
  />
  <NoteOverview
    v-for="child in childrenx"
    v-bind="{ noteId: child.note.id, level: 2 }"
    :key="child.note.id"
  />
</template>

<script lang="ts">
import NoteShow from "./NoteShow.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: Number,
    level: {type: Number, default: 1},
  },
  emits: ["updated"],
  components: { NoteShow },
  computed: {
    noteViewedByUser() { return this.$store.getters.getNoteById(this.noteId)},
    childrenx() { return this.$store.getters.getChildrenOfParentId(this.noteId)}
  }
};
</script>

<template>
  <NoteCard v-bind="{ note: noteViewedByUser, offset: offset }"/>
  <NoteMindmap
    v-for="(childId, index) in childrenIds"
    v-bind="{ noteId: childId, offset: 250 * (index % 2 == 0 ? 1 : -1) }"
    :key="childId"
  />
</template>

<script lang="ts">
import NoteCard from "./NoteCard.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: Number,
    offset: { type: Number, default: 0}
  },
  emits: ["updated"],
  components: { NoteCard },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.$store.getters.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

<template>
<div class="mindmap">
  <NoteCard v-bind="{ note: noteViewedByUser }"/>
  <NoteCard
    v-for="child in children"
    v-bind="{ note: child }"
    :key="child.id"
  />
</div>
</template>

<script lang="ts">
import NoteCard from "./NoteCard.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: Number,
  },
  emits: ["updated"],
  components: { NoteCard },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.noteId);
    },
  },
};
</script>

<style lang="sass" scoped>
.mindmap
  position: relative
  height: 100%
</style>

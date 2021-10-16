<template>
<div class="mindmap">
  <NoteCard v-bind="{ note: noteViewedByUser, offset: 0 }"/>
  <NoteCard
    v-for="(child, index) in children"
    v-bind="{ note: child, offset: 20 * (index % 2 == 0 ? 1 : -1) }"
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

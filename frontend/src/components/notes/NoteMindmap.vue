<template>
<div class="mindmap">
  <NoteCard v-bind="{ note: noteViewedByUser }"/>
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
    childrenIds() {
      return this.$store.getters.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

<style lang="sass" scoped>
.mindmap
  position: relative
  height: 100%
</style>

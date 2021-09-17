<template>
  <NoteShow
    v-bind="{ ...noteViewedByUser, level }"
    @updated="$emit('updated')"
  />
  <div class="note-list">
    <NoteOverview
      v-for="child in childrenx"
      v-bind="{ noteId: child.id, level: level + 1 }"
      :key="child.id"
    />
  </div>
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

<style lang="sass" scoped>
  .note-list
    margin-left: 10px

</style>
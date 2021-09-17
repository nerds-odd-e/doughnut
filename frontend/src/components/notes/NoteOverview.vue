<template>
  <NoteShow
    v-bind="{ ...noteViewedByUser }"
    @updated="$emit('updated')"
  />
  <div class="note-list">
    <NoteOverview
      v-for="child in childrenx"
      v-bind="{ noteId: child.id }"
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
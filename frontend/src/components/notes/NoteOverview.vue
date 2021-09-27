<template>
  <NoteShow v-bind="{ ...noteViewedByUser }"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in childrenIds"
      v-bind="{ noteId: childId }"
      :key="childId"
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
.note-list
  margin-left: 10px
</style>

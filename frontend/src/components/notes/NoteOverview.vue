<template>
  <NoteWithLinks v-bind="{ note, language }" v-if="note"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in childrenIds"
      v-bind="{ noteId: childId, expandChildren, language }"
      :key="childId"
      @highlight="$emit('highlight', $event)"
    />
  </div>
</template>

<script lang="ts">
import NoteWithLinks from "./NoteWithLinks.vue";

export default {
  name: "NoteOverview",
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
    language: String,
  },
  emits: ["highlight"],
  components: { NoteWithLinks },
  computed: {
    note() {
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

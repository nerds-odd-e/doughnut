<template>
  <NoteWithLinks v-bind="{ note }" v-if="note"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in childrenIds"
      v-bind="{ noteId: childId, expandChildren }"
      :key="childId"
    />
  </div>
</template>

<script lang="ts">
import NoteWithLinks from "./NoteWithLinks.vue";
import { useStore } from "@/store/index.js";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  name: "NoteOverview",
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
  },
  components: { NoteWithLinks },
  computed: {
    note() {
      return this.store.getNoteById(this.noteId);
    },
    childrenIds() {
      return this.store.getChildrenIdsByParentId(this.noteId);
    },
  },
};
</script>

<style lang="sass" scoped>
.note-list
  margin-left: 10px
</style>

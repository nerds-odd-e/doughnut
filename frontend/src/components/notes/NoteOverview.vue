<template>
  <NoteWithLinks v-bind="{ noteId }"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in childrenIds"
      v-bind="{ noteId: childId, expandChildren }"
      :key="childId"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import NoteWithLinks from "./NoteWithLinks.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteOverview",
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
  },
  components: { NoteWithLinks },
  computed: {
    childrenIds() {
      return this.piniaStore.getChildrenIdsByParentId(this.noteId);
    },
  },
});
</script>

<style lang="sass" scoped>
.note-list
  margin-left: 10px
</style>

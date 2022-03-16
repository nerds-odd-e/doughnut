<template>
<template v-if="noteSphere">
  <NoteWithLinks v-bind="{ note: noteSphere?.note, links: noteSphere.links }"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in noteSphere.childrenIds"
      v-bind="{ noteId: childId, expandChildren }"
      :key="childId"
    />
  </div>
</template>
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
    noteSphere() {
      return this.piniaStore.getNoteSphereById(this.noteId);
    },
  },
});
</script>

<style lang="sass" scoped>
.note-list
  margin-left: 10px
</style>

<template>
<template v-if="noteRealm">
  <NoteWithLinks v-bind="{ note: noteRealm?.note, links: noteRealm.links }"/>
  <div class="note-list">
    <NoteOverview
      v-for="childId in noteRealm.children"
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
    noteRealm() {
      return this.piniaStore.getNoteRealmById(this.noteId);
    },
  },
});
</script>

<style lang="sass" scoped>
.note-list
  margin-left: 10px
</style>

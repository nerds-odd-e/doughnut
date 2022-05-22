<template>
  <NoteRealmAsync
    v-bind="{
      noteId,
      viewType: 'mindmap',
      expandChildren: true,
    }"
    :key="noteId"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteRealmAsync from "../components/notes/NoteRealmAsync.vue";

export default defineComponent({
  props: { noteId: { type: Number, required: true } },
  components: { NoteRealmAsync },
});
</script>

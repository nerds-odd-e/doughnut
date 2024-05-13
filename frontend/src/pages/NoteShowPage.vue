<template>
  <div class="container">
    <NoteShow
      v-bind="{
        noteId,
        expandChildren: true,
        readonly: !user,
        storageAccessor,
      }"
      :key="noteId"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { User } from "@/generated/backend";
import NoteShow from "../components/notes/NoteShow.vue";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<User> },
  },
  components: { NoteShow },
});
</script>

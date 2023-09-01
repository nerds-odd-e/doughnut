<template>
  <div class="inner-box" :key="noteId">
    <div class="content">
      <div class="container">aaa</div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  methods: {
    async fetchData() {
      const noteWithDescendents =
        await this.api.noteMethods.getNoteWithDescendents(this.noteId);
      this.storageAccessor.selectPosition(
        noteWithDescendents.notes[0]?.note,
        noteWithDescendents.notePosition,
      );
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

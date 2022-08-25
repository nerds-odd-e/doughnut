<template>
  <button title="Delete note" @click="deleteNote">
    <SvgRemove />Delete note
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import usePopups from "../commons/Popups/usePopup";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  props: {
    noteId: { type: Number, required: true },
  },
  emits: ["noteDeleted"],
  components: {
    SvgRemove,
  },
  methods: {
    async deleteNote() {
      if (await this.popups.confirm(`Confirm to delete this note?`)) {
        const parentId = await this.storedApi.deleteNote(this.noteId);
        this.$emit("noteDeleted", parentId);
      }
    },
  },
});
</script>

<template>
  <button title="Delete note" @click="deleteNote">
    <SvgRemove />Delete note
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import usePopups from "../commons/Popups/usePopup";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  setup(props) {
    return {
      ...useStoredLoadingApi({ historyWriter: props.historyWriter }),
      ...usePopups(),
    };
  },
  props: {
    noteId: { type: Number, required: true },
    historyWriter: {
      type: Object as PropType<HistoryWriter>,
    },
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

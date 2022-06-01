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

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
  },
  emits: ["noteDeleted"],
  components: {
    SvgRemove,
  },
  methods: {
    async deleteNote() {
      if (await this.popups.confirm(`Confirm to delete this note?`)) {
        const { id, parentId } = this.note;
        await this.storedApi.deleteNote(id);
        if (parentId) {
          this.$emit("noteDeleted", id);
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
});
</script>

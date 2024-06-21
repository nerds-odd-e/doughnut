<template>
  <button title="Delete note" @click="deleteNote">
    <SvgRemove />Delete note
  </button>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { StorageAccessor } from "../../../store/createNoteStorage"
import usePopups from "../../commons/Popups/usePopups"
import SvgRemove from "../../svgs/SvgRemove.vue"

export default defineComponent({
  setup() {
    return {
      ...usePopups(),
    }
  },
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    SvgRemove,
  },
  methods: {
    async deleteNote() {
      if (await this.popups.confirm(`Confirm to delete this note?`)) {
        await this.storageAccessor
          .storedApi()
          .deleteNote(this.$router, this.noteId)
      }
    },
  },
})
</script>

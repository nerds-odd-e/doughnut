<template>
  <button
    class="btn"
    role="button"
    :title="undoTitle"
    @click="undoDelete()"
    v-if="history"
  >
    <SvgUndo />
  </button>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { StorageAccessor } from "../../store/createNoteStorage"
import SvgUndo from "../svgs/SvgUndo.vue"

export default defineComponent({
  components: {
    SvgUndo,
  },
  props: {
    noteId: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  computed: {
    history() {
      return this.storageAccessor.peekUndo()
    },
    undoTitle() {
      if (this.history) {
        return `undo ${this.history.type}`
      }
      return "undo"
    },
  },
  methods: {
    undoDelete() {
      this.storageAccessor.storedApi().undo(this.$router)
    },
  },
})
</script>

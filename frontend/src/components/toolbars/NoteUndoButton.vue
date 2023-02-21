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
import { defineComponent, PropType } from "vue";
import SvgUndo from "../svgs/SvgUndo.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

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
      return this.storageAccessor.peekUndo();
    },
    undoTitle() {
      if (this.history) {
        return `undo ${this.history.type}`;
      }
      return "undo";
    },
  },
  methods: {
    undoDelete() {
      this.storageAccessor.api(this.$router).undo();
    },
  },
});
</script>

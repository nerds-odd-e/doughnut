<template>
  <span
    v-if="!showInfo"
    @click="toggleStatistics()"
    class="info-button"
    role="button"
    title="statistics"
    width="100%"
  >
    i...
  </span>
  <NoteStatistics
    v-if="showInfo"
    :note-id="noteId"
    @level-changed="$emit('levelChanged', $event)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteStatistics from "./NoteStatistics.vue";

export default defineComponent({
  props: { noteId: { type: Number, required: true }, expanded: Boolean },
  emits: ["levelChanged"],
  components: { NoteStatistics },
  data() {
    return { showInfo: this.expanded as boolean };
  },
  methods: {
    toggleStatistics() {
      if (!this.showInfo) {
        this.showInfo = true;
      } else {
        this.showInfo = false;
      }
    },
  },
});
</script>

<style lang="scss" scoped>
.info-button {
  width: 100%;
  align-items: center;
  justify-content: center;
  font-size: smaller;
  cursor: pointer;
  display: block;
}
</style>

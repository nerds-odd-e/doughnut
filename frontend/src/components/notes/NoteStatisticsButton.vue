<template>
  <a
    @click="toggleStatistics()"
    class="btn btn-sm"
    role="button"
    title="statistics"
  >
    Statistics
  </a>
  <NoteStatistics v-if="showInfo" :note-id="noteId"></NoteStatistics>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteStatistics from "./NoteStatistics.vue";

export default defineComponent({
  props: { noteId: Number, expanded: Boolean },
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

<template>
  <span
    v-if="!noteInfo"
    @click="toggleNoteInfo()"
    role="button"
    title="note info"
    width="100%"
  >
    i...
  </span>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { NoteInfo } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import NoteInfoComponent from "./NoteInfoComponent.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: { noteId: { type: Number, required: true }, expanded: Boolean },
  emits: ["levelChanged"],
  components: { NoteInfoComponent },
  data() {
    return { noteInfo: undefined as undefined | NoteInfo };
  },
  methods: {
    fetchData() {
      this.managedApi.restNoteController
        .getNoteInfo(this.noteId)
        .then((articles) => {
          this.noteInfo = articles;
        });
    },

    toggleNoteInfo() {
      if (!this.noteInfo) {
        this.fetchData();
      } else {
        this.noteInfo = undefined;
      }
    },
  },
  mounted() {
    if (this.expanded) {
      this.fetchData();
    }
  },
});
</script>

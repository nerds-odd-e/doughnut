<template>
  <NoteAccessoryDisplay v-if="noteAccessory" :note-accessory="noteAccessory" />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { NoteAccessory } from "@/generated/backend";
import NoteAccessoryDisplay from "./NoteAccessoryDisplay.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    noteAccessory: { type: Object as PropType<NoteAccessory> },
  },
  components: {
    NoteAccessoryDisplay,
  },
  data() {
    return {
      noteAccessory1: undefined as NoteAccessory | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.noteAccessory1 =
        await this.managedApi.restNoteController.showNoteAccessory(this.noteId);
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

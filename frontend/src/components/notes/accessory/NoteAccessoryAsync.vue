<template>
  <div class="note-accessory text-break">
    <NoteAccessoryToolbar
      v-if="!readonly"
      v-bind="{ noteId }"
      @note-accessory-updated="noteAccessoryUpdated"
    />
    <NoteAccessoryDisplay
      v-if="noteAccessory"
      :note-accessory="noteAccessory"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { NoteAccessory } from "@/generated/backend";
import NoteAccessoryDisplay from "./NoteAccessoryDisplay.vue";
import NoteAccessoryToolbar from "./NoteAccessoryToolbar.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    readonly: { type: Boolean, required: true },
  },
  components: {
    NoteAccessoryDisplay,
    NoteAccessoryToolbar,
  },
  data() {
    return {
      noteAccessory: undefined as NoteAccessory | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.noteAccessory =
        await this.managedApi.restNoteController.showNoteAccessory(this.noteId);
    },
    noteAccessoryUpdated() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

<style scoped>
.note-accessory {
  background-color: #f0f0f0;
  height: 100%;
}
</style>

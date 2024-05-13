<template>
  <form v-if="noteAccessory" @submit.prevent.once="processForm">
    <NoteFormBody
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { NoteAccessoriesDTO, NoteAccessory } from "@/generated/backend";
import NoteFormBody from "./NoteFormBody.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  components: {
    NoteFormBody,
  },
  props: {
    noteId: { type: Number, required: true },
  },
  emits: ["closeDialog"],
  data() {
    return {
      noteAccessory: undefined as NoteAccessory | undefined,
      formData: {} as NoteAccessoriesDTO,
      noteFormErrors: {},
    };
  },

  methods: {
    async fetchData() {
      this.noteAccessory =
        (await this.managedApi.restNoteController.showNoteAccessory(
          this.noteId,
        )) || {};
      this.formData = { ...this.noteAccessory };
    },
    processForm() {
      this.managedApi.restNoteController
        .updateNoteAccessories(this.noteId, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

<template>
  <form v-if="noteAccessory" @submit.prevent.once="processForm">
    <ImageFormBody
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Submit" class="daisy:btn daisy:btn-primary" />
  </form>
</template>

<script lang="ts">
import type { NoteAccessoriesDTO, NoteAccessory } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { defineComponent } from "vue"
import ImageFormBody from "./ImageFormBody.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  components: {
    ImageFormBody,
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
    }
  },

  methods: {
    async fetchData() {
      this.noteAccessory =
        (await this.managedApi.restNoteController.showNoteAccessory(
          this.noteId
        )) || {}
      this.formData = { ...this.noteAccessory }
    },
    processForm() {
      this.managedApi.restNoteController
        .updateNoteAccessories(this.noteId, this.formData)
        .then((na) => this.$emit("closeDialog", na))
        .catch((error) => {
          this.noteFormErrors = error
        })
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>

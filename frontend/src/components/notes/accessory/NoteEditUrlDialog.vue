<template>
  <form v-if="noteAccessory" @submit.prevent.once="processForm">
    <UrlFormBody
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import type { NoteAccessoriesDto, NoteAccessory } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { defineComponent } from "vue"
import UrlFormBody from "./UrlFormBody.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  components: {
    UrlFormBody,
  },
  props: {
    noteId: { type: Number, required: true },
  },
  emits: ["closeDialog"],
  data() {
    return {
      noteAccessory: undefined as NoteAccessory | undefined,
      formData: {} as NoteAccessoriesDto,
      noteFormErrors: {},
    }
  },

  methods: {
    async fetchData() {
      this.noteAccessory =
        (await this.managedApi.services.showNoteAccessory({
          path: { note: this.noteId },
        })) || {}
      this.formData = { ...this.noteAccessory }
    },
    processForm() {
      this.managedApi.services
        .updateNoteAccessories({
          path: { note: this.noteId },
          body: this.formData,
        })
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

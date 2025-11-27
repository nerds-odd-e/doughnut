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
import { NoteController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { defineComponent } from "vue"
import UrlFormBody from "./UrlFormBody.vue"

export default defineComponent({
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
      noteFormErrors: {} as Record<string, string>,
    }
  },

  methods: {
    async fetchData() {
      const { data: accessory, error } = await NoteController.showNoteAccessory(
        {
          path: { note: this.noteId },
        }
      )
      if (!error) {
        this.noteAccessory = accessory!
        this.formData = { ...this.noteAccessory }
      }
    },
    async processForm() {
      const { data: updatedAccessory, error } =
        await NoteController.updateNoteAccessories({
          path: { note: this.noteId },
          body: this.formData,
        })
      if (error) {
        const errorObj = toOpenApiError(error)
        this.noteFormErrors = errorObj.errors || {}
      } else {
        this.$emit("closeDialog", updatedAccessory)
      }
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>

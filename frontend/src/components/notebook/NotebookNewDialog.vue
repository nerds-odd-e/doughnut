<template>
  <form @submit.prevent.once="processForm">
    <NoteFormTitleOnly
      v-model="noteFormData.newTitle"
      :error-message="errors.newTitle"
    />
    <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import NoteFormTitleOnly from "@/components/notes/NoteFormTitleOnly.vue"
import type { Circle, NoteCreationDto } from "@generated/backend"
import {
  createNotebookInCircle,
  createNotebook,
} from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import type { PropType } from "vue"

export default {
  props: { circle: { type: Object as PropType<Circle> } },
  components: {
    NoteFormTitleOnly,
  },
  data() {
    return {
      noteFormData: { newTitle: "" } as NoteCreationDto,
      errors: { newTitle: undefined as undefined | string } as Record<
        string,
        string | undefined
      >,
    }
  },
  methods: {
    async processForm() {
      const { data: result, error } = this.circle
        ? await createNotebookInCircle({
            path: { circle: this.circle.id },
            body: this.noteFormData,
          })
        : await createNotebook({
            body: this.noteFormData,
          })
      if (!error) {
        this.$router.push({
          name: "noteShow",
          params: { noteId: result!.noteId },
        })
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract field-level errors if available (for 400 validation errors)
        const errorObj = toOpenApiError(error)
        this.errors = { ...this.errors, ...(errorObj.errors || {}) }
      }
    },
  },
}
</script>

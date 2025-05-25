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
import type { Circle, NoteCreationDTO } from "generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"

export default {
  setup() {
    return useLoadingApi()
  },
  props: { circle: { type: Object as PropType<Circle> } },
  components: {
    NoteFormTitleOnly,
  },
  data() {
    return {
      noteFormData: { newTitle: "" } as NoteCreationDTO,
      errors: { newTitle: undefined as undefined | string },
    }
  },
  methods: {
    createNotebook() {
      if (this.circle) {
        return this.managedApi.restCircleController.createNotebookInCircle(
          this.circle.id,
          this.noteFormData
        )
      }
      return this.managedApi.restNotebookController.createNotebook(
        this.noteFormData
      )
    },
    processForm() {
      this.createNotebook()
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          })
        )
        .catch((err) => (this.errors = err))
    },
  },
}
</script>

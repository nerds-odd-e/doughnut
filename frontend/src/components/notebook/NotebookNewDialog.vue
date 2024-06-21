<template>
  <form @submit.prevent.once="processForm">
    <NoteFormTopicOnly
      v-model="noteFormData.topicConstructor"
      :errors="errors.topicConstructor"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import NoteFormTopicOnly from "@/components/notes/NoteFormTopicOnly.vue"
import { Circle, NoteCreationDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType } from "vue"

export default {
  setup() {
    return useLoadingApi()
  },
  props: { circle: { type: Object as PropType<Circle> } },
  components: {
    NoteFormTopicOnly,
  },
  data() {
    return {
      noteFormData: { topicConstructor: "" } as NoteCreationDTO,
      errors: { topicConstructor: undefined as undefined | string },
    }
  },
  methods: {
    createNotebook() {
      if (this.circle) {
        return this.managedApi.restCircleController.createNotebookInCircle(
          this.circle.id,
          this.noteFormData,
        )
      }
      return this.managedApi.restNotebookController.createNotebook(
        this.noteFormData,
      )
    },
    processForm() {
      this.createNotebook()
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          }),
        )
        .catch((err) => (this.errors = err))
    },
  },
}
</script>

<template>
  <div class="daisy-btn-group daisy-btn-group-sm daisy-flex daisy-align-items-center daisy-flex-wrap">
    <BazaarNotebookButtons v-if="notebook.circle" :notebook="notebook" :logged-in="true" />
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="editNotebookSettings"
      title="Edit notebook settings"
    >
      <BookText class="w-5 h-5" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"
import { BookText } from "lucide-vue-next"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"

const router = useRouter()

const props = defineProps<{
  notebook: Notebook
  user?: User
}>()

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const editNotebookSettings = () => {
  router.push({
    name: "notebookEdit",
    params: { notebookId: props.notebook.id },
  })
}
</script>

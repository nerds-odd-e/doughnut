<template>
  <div class="daisy-btn-group daisy-btn-group-sm daisy-flex daisy-align-items-center daisy-flex-wrap">
    <BazaarNotebookButtons v-if="notebook.circle" :notebook="notebook" :logged-in="true" />
    <details
      ref="actionsDropdown"
      class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
    >
      <summary
        data-cy="notebook-catalog-overflow"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none daisy-cursor-pointer"
        aria-label="Notebook actions"
      >
        <MoreHorizontal class="w-5 h-5" />
      </summary>
      <ul
        tabindex="0"
        class="daisy-dropdown-content daisy-menu daisy-bg-base-100 daisy-rounded-box daisy-w-52 daisy-p-2 daisy-shadow daisy-z-[1000]"
      >
        <li class="daisy-menu-item daisy-p-0">
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-h-auto daisy-min-h-0 daisy-w-full daisy-justify-start daisy-py-2 daisy-font-normal"
            title="Edit notebook settings"
            @click="onEditNotebookSettings"
          >
            Edit notebook settings
          </button>
        </li>
      </ul>
    </details>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useRouter } from "vue-router"
import { MoreHorizontal } from "lucide-vue-next"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"

const router = useRouter()

const props = defineProps<{
  notebook: Notebook
  user?: User
}>()

defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const actionsDropdown = ref<HTMLDetailsElement | null>(null)

const onEditNotebookSettings = () => {
  if (actionsDropdown.value) {
    actionsDropdown.value.open = false
  }
  router.push({
    name: "notebookEdit",
    params: { notebookId: props.notebook.id },
  })
}
</script>

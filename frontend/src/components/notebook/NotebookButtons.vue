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
        <li class="daisy-menu-item daisy-p-0">
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-h-auto daisy-min-h-0 daisy-w-full daisy-justify-start daisy-py-2 daisy-font-normal"
            title="Move to group"
            @click="openMoveToGroup"
          >
            Move to group…
          </button>
        </li>
      </ul>
    </details>
    <Modal v-if="showMoveToGroup" @close_request="closeMoveToGroup">
      <template #body>
        <NotebookCatalogMoveToGroupDialog
          mode="owned"
          :notebook-id="notebook.id"
          :catalog-group-id="catalogGroupId"
          @close="closeMoveToGroup"
          @success="onMoveToGroupSuccess"
        />
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useRouter } from "vue-router"
import { MoreHorizontal } from "lucide-vue-next"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"
import Modal from "@/components/commons/Modal.vue"
import NotebookCatalogMoveToGroupDialog from "@/components/notebook/NotebookCatalogMoveToGroupDialog.vue"

const router = useRouter()

const props = defineProps<{
  notebook: Notebook
  user?: User
  catalogGroupId?: number
}>()

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

const actionsDropdown = ref<HTMLDetailsElement | null>(null)
const showMoveToGroup = ref(false)

const closeDropdown = () => {
  if (actionsDropdown.value) {
    actionsDropdown.value.open = false
  }
}

const onEditNotebookSettings = () => {
  closeDropdown()
  router.push({
    name: "notebookEdit",
    params: { notebookId: props.notebook.id },
  })
}

const openMoveToGroup = () => {
  closeDropdown()
  showMoveToGroup.value = true
}

const closeMoveToGroup = () => {
  showMoveToGroup.value = false
}

const onMoveToGroupSuccess = () => {
  showMoveToGroup.value = false
  emit("refresh")
}
</script>

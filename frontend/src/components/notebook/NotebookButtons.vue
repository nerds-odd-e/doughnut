<template>
  <div class="daisy-btn-group daisy-btn-group-sm flex daisy-align-items-center flex-wrap">
    <BazaarNotebookButtons v-if="notebook.circle" :notebook="notebook" :logged-in="true" />
    <button
      v-if="hasAttachedBook === true"
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Read book"
      aria-label="Read book"
      data-testid="notebook-catalog-read-book"
      @click="onReadBook"
    >
      <BookOpen class="h-6 w-6" />
    </button>
    <AutoCollapseDropdown
      v-slot="{ closeDropdown }"
      class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
    >
      <summary
        data-cy="notebook-catalog-overflow"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer"
        aria-label="Notebook actions"
      >
        <MoreHorizontal class="h-6 w-6" />
      </summary>
      <ul
        tabindex="0"
        class="daisy-dropdown-content daisy-menu bg-base-100 rounded-box w-52 p-2 shadow z-[1000]"
      >
        <li class="daisy-menu-item p-0">
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost h-auto min-h-0 w-full justify-start py-2 font-normal"
            title="Move to group"
            @click="openMoveToGroup(closeDropdown)"
          >
            Move to group…
          </button>
        </li>
      </ul>
    </AutoCollapseDropdown>
    <Modal v-if="showMoveToGroup" @close_request="closeMoveToGroup">
      <template #body>
        <NotebookCatalogMoveToGroupForm
          mode="owned"
          :notebook-id="notebook.id"
          :catalog-group-id="catalogGroupId"
          :existing-groups="moveToGroupExistingGroups"
          :circle-id="moveToGroupCircleId"
          @close="closeMoveToGroup"
          @success="onMoveToGroupSuccess"
        />
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, type ComputedRef } from "vue"
import { useRouter } from "vue-router"
import { BookOpen, MoreHorizontal } from "lucide-vue-next"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import Modal from "@/components/commons/Modal.vue"
import {
  catalogMoveToGroupContextKey,
  type CatalogMoveToGroupInjected,
} from "@/components/notebook/catalogMoveToGroupContext"
import NotebookCatalogMoveToGroupForm from "@/components/notebook/NotebookCatalogMoveToGroupForm.vue"

const router = useRouter()

const catalogMoveToGroupContext = inject<
  ComputedRef<CatalogMoveToGroupInjected | undefined> | undefined
>(catalogMoveToGroupContextKey, undefined)

const moveToGroupExistingGroups = computed(() => {
  const ctx = catalogMoveToGroupContext?.value
  return ctx?.existingGroups
})

const moveToGroupCircleId = computed(
  () => catalogMoveToGroupContext?.value?.circleId
)

const props = defineProps<{
  notebook: Notebook
  hasAttachedBook?: boolean
  user?: User
  catalogGroupId?: number
}>()

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

const showMoveToGroup = ref(false)

const onReadBook = () => {
  router.push({
    name: "bookReading",
    params: { notebookId: props.notebook.id },
  })
}

const openMoveToGroup = (closeDropdown: () => void) => {
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

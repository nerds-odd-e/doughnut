<template>
  <AutoCollapseDropdown
    v-slot="{ closeDropdown }"
    class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom shrink-0"
  >
    <summary
      data-cy="notebook-group-overflow"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer"
      aria-label="Group actions"
    >
      <MoreHorizontal class="h-6 w-6" />
    </summary>
    <DropdownMenu>
      <DropdownMenuItem>
        <button
          type="button"
          :class="dropdownMenuButtonClass"
          title="Add notebook"
          data-testid="notebook-group-add-notebook"
          @click="openAddNotebook(closeDropdown)"
        >
          Add notebook…
        </button>
      </DropdownMenuItem>
    </DropdownMenu>
  </AutoCollapseDropdown>
  <Modal v-if="showAddNotebook" @close_request="closeAddNotebook">
    <template #body>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="notebook-group-add-notebook-title"
        class="px-1"
      >
        <h2
          id="notebook-group-add-notebook-title"
          class="m-0 mb-3 text-xl font-semibold"
        >
          New notebook
        </h2>
        <NotebookNewForm
          :circle="circle"
          :notebook-group="{ id: group.id, name: group.name }"
        />
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, type ComputedRef, type PropType } from "vue"
import { MoreHorizontal } from "@lucide/vue"
import type {
  Circle,
  NotebookCatalogGroupItem,
} from "@generated/doughnut-backend-api"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
import Modal from "@/components/commons/Modal.vue"
import {
  catalogMoveToGroupContextKey,
  type CatalogMoveToGroupInjected,
} from "./catalogMoveToGroupContext"
import NotebookNewForm from "./NotebookNewForm.vue"

defineProps({
  group: {
    type: Object as PropType<NotebookCatalogGroupItem>,
    required: true,
  },
})

const catalogMoveToGroupContext = inject<
  ComputedRef<CatalogMoveToGroupInjected | undefined> | undefined
>(catalogMoveToGroupContextKey, undefined)

const circle = computed((): Circle | undefined => {
  const circleId = catalogMoveToGroupContext?.value?.circleId
  if (circleId === undefined) {
    return undefined
  }
  return { id: circleId } as Circle
})

const showAddNotebook = ref(false)

function openAddNotebook(closeDropdown: () => void) {
  closeDropdown()
  showAddNotebook.value = true
}

function closeAddNotebook() {
  showAddNotebook.value = false
}
</script>

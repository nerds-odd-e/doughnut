<template>
  <div class="daisy-join flex items-center">
    <AutoCollapseDropdown
      v-slot="{ closeDropdown }"
      class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
    >
      <summary
        data-cy="notebook-catalog-overflow"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer"
        aria-label="Notebook actions"
      >
        <MoreHorizontal class="w-6 h-6" />
      </summary>
      <DropdownMenu>
        <DropdownMenuItem>
          <button
            type="button"
            :class="dropdownMenuButtonClass"
            title="Edit subscription"
            @click="openEdit(closeDropdown)"
          >
            Edit subscription
          </button>
        </DropdownMenuItem>
        <DropdownMenuItem>
          <button
            type="button"
            :class="dropdownMenuButtonClass"
            title="Move to group"
            @click="openMoveToGroup(closeDropdown)"
          >
            Move to group…
          </button>
        </DropdownMenuItem>
      </DropdownMenu>
    </AutoCollapseDropdown>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Unsubscribe"
      @click="processForm"
    >
      <Minus class="w-6 h-6" />
    </button>
    <Modal v-if="showEdit" @close_request="closeEdit">
      <template #body>
        <SubscriptionEditForm :subscription="subscription" />
      </template>
    </Modal>
    <Modal v-if="showMoveToGroup" @close_request="closeMoveToGroup">
      <template #body>
        <NotebookCatalogMoveToGroupForm
          mode="subscribed"
          :notebook-id="notebookId"
          :subscription-id="subscription.id"
          :catalog-group-id="catalogGroupId"
          @close="closeMoveToGroup"
          @success="onMoveToGroupSuccess"
        />
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type {
  Subscription,
  SubscriptionForNotebooksListing,
} from "@generated/doughnut-backend-api"
import { SubscriptionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import Modal from "../commons/Modal.vue"
import AutoCollapseDropdown from "../commons/AutoCollapseDropdown.vue"
import DropdownMenu from "../commons/DropdownMenu.vue"
import DropdownMenuItem from "../commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "../commons/dropdownMenuClasses"
import usePopups from "../commons/Popups/usePopups"
import { Minus, MoreHorizontal } from "@lucide/vue"
import NotebookCatalogMoveToGroupForm from "@/components/notebook/NotebookCatalogMoveToGroupForm.vue"
import SubscriptionEditForm from "./SubscriptionEditForm.vue"

const props = defineProps({
  subscription: {
    type: Object as PropType<Subscription | SubscriptionForNotebooksListing>,
    required: true,
  },
  notebookId: {
    type: Number,
    required: true,
  },
  catalogGroupId: {
    type: Number as PropType<number | undefined>,
    default: undefined,
  },
})

const emit = defineEmits<{
  (e: "updated"): void
}>()

const { popups } = usePopups()
const showEdit = ref(false)
const showMoveToGroup = ref(false)

const openEdit = (closeDropdown: () => void) => {
  closeDropdown()
  showEdit.value = true
}

const closeEdit = () => {
  showEdit.value = false
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
  emit("updated")
}

const processForm = async () => {
  if (await popups.confirm(`Confirm to unsubscribe from this notebook?`)) {
    const { error } = await apiCallWithLoading(() =>
      SubscriptionController.destroySubscription({
        path: { subscription: props.subscription.id },
      })
    )
    if (!error) {
      emit("updated")
    }
  }
}
</script>

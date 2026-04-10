<template>
  <div class="daisy-join daisy-flex daisy-items-center">
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
            title="Edit subscription"
            @click="openEdit"
          >
            Edit subscription
          </button>
        </li>
      </ul>
    </details>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Unsubscribe"
      @click="processForm"
    >
      <Minus class="w-5 h-5" />
    </button>
    <Modal v-if="showEdit" @close_request="closeEdit">
      <template #body>
        <SubscriptionEditDialog :subscription="subscription" />
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Subscription } from "@generated/doughnut-backend-api"
import { SubscriptionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import Modal from "../commons/Modal.vue"
import usePopups from "../commons/Popups/usePopups"
import { Minus, MoreHorizontal } from "lucide-vue-next"
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue"

const props = defineProps({
  subscription: {
    type: Object as PropType<Subscription>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "updated"): void
}>()

const { popups } = usePopups()
const actionsDropdown = ref<HTMLDetailsElement | null>(null)
const showEdit = ref(false)

const closeDropdown = () => {
  if (actionsDropdown.value) {
    actionsDropdown.value.open = false
  }
}

const openEdit = () => {
  closeDropdown()
  showEdit.value = true
}

const closeEdit = () => {
  showEdit.value = false
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

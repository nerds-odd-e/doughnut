<template>
  <Teleport to="body">
    <dialog
      v-if="show"
      ref="dialogRef"
      class="loading-modal-mask"
      @cancel.prevent
    >
      <div class="loading-modal-content">
        <div class="daisy-loading daisy-loading-spinner daisy-loading-lg"></div>
        <p class="loading-message">{{ message }}</p>
        <IdentityBoundCancelButton
          v-if="cancelControl"
          :key="cancelControl.id"
          :cancel-action="cancelControl.action"
        />
      </div>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type { ApiLoadingCancelControl } from "@/managedApi/ApiStatusHandler"
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { defineComponent, h, ref, toRef, type PropType } from "vue"

const IdentityBoundCancelButton = defineComponent({
  props: {
    cancelAction: {
      type: Function as PropType<() => void>,
      required: true,
    },
  },
  setup(props) {
    const stateBoundAction = props.cancelAction
    return () =>
      h(
        "button",
        {
          type: "button",
          class:
            "daisy-btn daisy-btn-ghost text-white focus-visible:outline-2 focus-visible:outline-white",
          onClick: stateBoundAction,
        },
        "Cancel"
      )
  },
})

interface Props {
  show: boolean
  message?: string
  cancelControl?: ApiLoadingCancelControl
}

const props = withDefaults(defineProps<Props>(), {
  message: "Processing...",
})

const dialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(toRef(props, "show"), dialogRef)
</script>

<style scoped>
dialog.loading-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  padding: 0;
  margin: 0;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  align-items: safe center;
  justify-content: center;
  overflow-y: auto;
}

.loading-modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.daisy-loading-spinner {
  color: white;
}

.loading-message {
  color: white;
  font-size: 1.125rem;
  font-weight: 500;
  margin: 0;
}
</style>

<style>
dialog.loading-modal-mask::backdrop {
  background-color: rgba(0, 0, 0, 0.7);
}
</style>

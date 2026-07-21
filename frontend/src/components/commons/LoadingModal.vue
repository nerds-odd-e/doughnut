<template>
  <Overlay v-if="show" class="loading-modal-mask" centered dark :z-index="10000">
    <div class="loading-modal-content">
      <div class="daisy-loading daisy-loading-spinner daisy-loading-lg"></div>
      <p class="loading-message">{{ message }}</p>
      <IdentityBoundCancelButton
        v-if="cancelAction"
        :key="loadingStateId"
        :cancel-action="cancelAction"
      />
    </div>
  </Overlay>
</template>

<script setup lang="ts">
import { defineComponent, h, type PropType } from "vue"
import Overlay from "./Overlay.vue"

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
  loadingStateId?: number
  cancelAction?: () => void
}

withDefaults(defineProps<Props>(), {
  message: "Processing...",
})
</script>

<style scoped>
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

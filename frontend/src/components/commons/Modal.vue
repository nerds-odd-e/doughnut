<template>
  <Teleport to="body">
    <dialog
      ref="dialogRef"
      class="modal-mask text-base-content"
      :class="{ 'modal-align-top': alignTop }"
      @cancel.prevent
    >
    <div class="modal-panel-wrapper" @mousedown.self="$emit('close_request')">
      <div
        :class="[
          sidebarStyle,
          'bg-base-200',
          { 'modal-panel--no-close': !showCloseButton },
        ]"
      >
        <button
          v-if="showCloseButton"
          class="close-button"
          @click="$emit('close_request')"
        >
          <X class="w-6 h-6" />
        </button>

        <div v-if="$slots.header" class="modal-header">
          <slot name="header" />
        </div>

        <div class="modal-body">
          <slot name="body" />
        </div>
      </div>
    </div>
  </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from "vue"
import { X } from "lucide-vue-next"
import { useRoute } from "vue-router"
import { registerModal } from "./modalStack"
import { focusAutofocusTargetWithin } from "@/utils/focusTarget"

// Props
interface Props {
  sidebar?: "left" | "right"
  isPopup?: boolean
  alignTop?: boolean
  /** When false, the overlay X is omitted (e.g. in-dialog close control). */
  showCloseButton?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  showCloseButton: true,
})

// Emits
const emit = defineEmits<{
  close_request: []
}>()

const dialogRef = ref<HTMLDialogElement | null>(null)

// Computed
const sidebarStyle = computed(() => {
  if (props.sidebar === "left") return "modal-sidebar modal-left"
  if (props.sidebar === "right") return "modal-sidebar modal-right"
  return "modal-container"
})

// Route watcher
const route = useRoute()
watch(
  () => route.fullPath,
  () => {
    emit("close_request")
  }
)

// Open as modal and register ESC handler for non-popup modals
let unregister: (() => void) | undefined
onMounted(() => {
  try {
    dialogRef.value?.showModal()
    requestAnimationFrame(() => {
      focusAutofocusTargetWithin(dialogRef.value)
    })
  } catch {
    // Dialog not connected to document (e.g. Teleport stubbed in tests)
  }
  if (!props.isPopup) {
    unregister = registerModal(() => emit("close_request"))
  }
})

onUnmounted(() => {
  unregister?.()
})
</script>

<style scoped lang="scss">
dialog.modal-mask {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  padding: 0;
  margin: 0;
  border: none;
  background: transparent;
  display: table;
  transition: opacity 0.3s ease;
}

.modal-panel-wrapper {
  display: table-cell;
  vertical-align: middle;
}

dialog.modal-align-top .modal-panel-wrapper {
  vertical-align: top;
  padding-top: max(env(safe-area-inset-top, 0px), 20px);
}

.modal-container {
  position: relative;
  max-width: 700px;
  max-height: 100vh;
  max-height: 100dvh;
  overflow: auto;
  margin: 0px auto;
  padding: 20px 30px;
  border-radius: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.33);
  transition: all 0.3s ease;
}

.modal-sidebar {
  position: relative;
  height: 100vh;
  height: 100dvh;
  overflow: auto;
  padding: 0px 0px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.33);
  transition: all 0.3s ease;
  .modal-body {
    position: initial;
    margin: 0;
    padding: 0;
  }
}

.modal-left {
  max-width: 300px;
  margin-left: 0px;
}

.modal-right {
  max-width: calc(100% - 50px);
  margin-right: 0px;
  margin-left: auto;
}

.modal-header h3 {
  margin-top: 0;
  color: #42b983;
}

.modal-body {
  margin: 20px 0;
}

.modal-container.modal-panel--no-close {
  padding-top: 12px;
}

.modal-panel--no-close .modal-body {
  margin-top: 8px;
  margin-bottom: 20px;
}

.close-button {
  position: absolute;
  right: 0.3em;
  top: 0.3em;
  width: 26px;
  padding: 1px;
  height: 26px;
  border: none;
  background: none;
}
</style>

<style lang="scss">
dialog.modal-mask::backdrop {
  background-color: rgba(0, 0, 0, 0.5);
  transition: opacity 0.3s ease;
}
</style>

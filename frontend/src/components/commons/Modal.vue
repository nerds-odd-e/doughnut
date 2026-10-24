<template>
  <Teleport to="body">
    <dialog
      ref="dialogRef"
      :aria-label="ariaLabel"
      class="modal-mask text-base-content"
      :class="{ 'modal-align-top': stableTopAnchored }"
      @cancel.prevent
    >
    <div class="modal-panel-wrapper" @mousedown.self="$emit('close_request')">
      <div
        :class="[
          sidebarStyle,
          'bg-base-200',
          { 'modal-panel--no-close': !showCloseButton },
          { 'modal-panel--scroll-body': scrollBody },
        ]"
      >
        <button
          v-if="showCloseButton"
          class="close-button"
          aria-label="Close modal"
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
import { X } from "@lucide/vue"
import { useRoute } from "vue-router"
import { registerModal } from "./modalStack"
import {
  focusAutofocusTargetWithin,
  prepareAutofocusTargetWithin,
} from "@/utils/focusTarget"
import { provideModalTopAnchor } from "@/composables/modalTopAnchor"

// Props
interface Props {
  ariaLabel?: string
  sidebar?: "left" | "right"
  isPopup?: boolean
  /** When false, the overlay X is omitted (e.g. in-dialog close control). */
  showCloseButton?: boolean
  /** When false, a route replace does not dismiss (e.g. a dialog bound to the location). */
  closeOnRouteChange?: boolean
  /** Keep the heading and Close visible while the body scrolls. */
  scrollBody?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  showCloseButton: true,
  closeOnRouteChange: true,
})

// Emits
const emit = defineEmits<{
  close_request: []
}>()

const dialogRef = ref<HTMLDialogElement | null>(null)
const stableTopAnchored = provideModalTopAnchor()

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
    if (!props.closeOnRouteChange) {
      return
    }
    emit("close_request")
  }
)

// Open as modal and register ESC handler for non-popup modals
let unregister: (() => void) | undefined
const openDialog = () => {
  prepareAutofocusTargetWithin(dialogRef.value)
  try {
    dialogRef.value?.showModal()
  } catch {
    // Dialog not connected to document (e.g. Teleport stubbed in tests)
    return
  }
  focusAutofocusTargetWithin(dialogRef.value)
}

onMounted(() => {
  openDialog()
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

.modal-panel--scroll-body {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 40px);
  overflow: clip;

  .modal-header {
    flex-shrink: 0;
  }

  .modal-body {
    flex: 1;
    min-height: 0;
    overflow: auto;
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

<template>
  <Teleport to="body">
    <div class="modal-mask daisy-text-base-content">
      <div class="modal-wrapper" @mousedown.self="$emit('close_request')">
        <div :class="sidebarStyle" class="daisy-bg-base-200">
          <button class="close-button" @click="$emit('close_request')">
            <SvgClose />
          </button>

          <div class="modal-header" v-if="$slots.header">
            <slot name="header" />
          </div>

          <div class="modal-body">
            <slot name="body" />
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, watch, onMounted, onUnmounted } from "vue"
import SvgClose from "../svgs/SvgClose.vue"
import { useRoute } from "vue-router"

// Props
interface Props {
  sidebar?: "left" | "right"
  isPopup?: boolean
}
const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  close_request: []
}>()

// Computed
const sidebarStyle = computed(() => {
  if (props.sidebar === "left") return "modal-sidebar modal-left"
  if (props.sidebar === "right") return "modal-sidebar modal-right"
  return "modal-container"
})

// Route watcher
const route = useRoute()
watch(route, () => {
  emit("close_request")
})

// ESC key handler - only for non-popup modals
const handleEscape = (event: KeyboardEvent) => {
  if (!props.isPopup && event.key === "Escape") {
    emit("close_request")
  }
}

// Add/remove event listener
onMounted(() => {
  if (!props.isPopup) {
    document.addEventListener("keydown", handleEscape)
  }
})

onUnmounted(() => {
  if (!props.isPopup) {
    document.removeEventListener("keydown", handleEscape)
  }
})
</script>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  z-index: 9990;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: table;
  transition: opacity 0.3s ease;
}

.modal-wrapper {
  display: table-cell;
  vertical-align: middle;
}

.modal-container {
  position: relative;
  max-width: 700px;
  max-height: 100vh;
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

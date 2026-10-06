<template>
  <div>
    <button class="fullscreen-btn" @click="toggleFullscreen">
      <Maximize :size="18" />
      Full screen
    </button>
    <Teleport to="body" v-if="isFullscreen">
      <div class="fullscreen-overlay">
        <button class="exit-fullscreen-btn" @click="exitFullscreen">
          Exit Full Screen
        </button>
        <slot></slot>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from "vue"
import { Maximize } from "@lucide/vue"

const emit = defineEmits(["fullscreenChange"])
const isFullscreen = ref(false)

const toggleFullscreen = async () => {
  if (!isFullscreen.value) {
    try {
      if (
        (
          document.documentElement as HTMLElement & {
            webkitRequestFullscreen(): Promise<void>
          }
        ).webkitRequestFullscreen
      ) {
        await (
          document.documentElement as HTMLElement & {
            webkitRequestFullscreen(): Promise<void>
          }
        ).webkitRequestFullscreen()
      } else if (document.documentElement.requestFullscreen) {
        await document.documentElement.requestFullscreen()
      }

      if (
        document.fullscreenElement ||
        (document as Document & { webkitFullscreenElement: Element | null })
          .webkitFullscreenElement
      ) {
        if (document.documentElement.requestPointerLock) {
          document.documentElement.requestPointerLock()
        }
        isFullscreen.value = true
        emit("fullscreenChange", true)
      }
    } catch (error) {
      console.error("Fullscreen failed:", error)
    }
  } else {
    await exitFullscreen()
  }
}

const exitFullscreen = async () => {
  if (!isFullscreen.value) return

  try {
    if (
      (document as Document & { webkitFullscreenElement: Element | null })
        .webkitFullscreenElement
    ) {
      await (
        document as Document & { webkitExitFullscreen(): Promise<void> }
      ).webkitExitFullscreen()
    } else if (document.fullscreenElement) {
      await document.exitFullscreen()
    }

    if (document.pointerLockElement) {
      document.exitPointerLock()
    }
  } finally {
    isFullscreen.value = false
    emit("fullscreenChange", false)
  }
}

onUnmounted(async () => {
  if (isFullscreen.value) {
    await exitFullscreen()
  }
})
</script>

<style scoped>
.fullscreen-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  background-color: black;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.exit-fullscreen-btn {
  padding: 12px 24px;
  background-color: #4299e1;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.exit-fullscreen-btn:hover {
  background-color: #3182ce;
}
</style>

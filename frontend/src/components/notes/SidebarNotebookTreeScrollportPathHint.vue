<template>
  <div
    v-if="clippedFolders.length > 0"
    class="scrollport-path-sticky-anchor"
    data-testid="sidebar-scrollport-path-hint-anchor"
  >
    <div
      class="scrollport-path-hint flex flex-row items-center gap-1 min-w-0 px-1 py-1 text-sm"
      role="region"
      aria-label="Ancestor folders scrolled out of view"
      data-testid="sidebar-scrollport-path-hint"
    >
      <ChevronUp :size="14" class="hint-chevron shrink-0" aria-hidden="true" />
      <span class="hint-links min-w-0 flex flex-wrap items-center">
        <template v-for="(folder, i) in clippedFolders" :key="folder.id">
          <span v-if="i > 0" class="hint-sep" aria-hidden="true">·</span>
          <RouterLink
            class="hint-link"
            :to="{
              name: 'folderPage',
              params: {
                notebookId: String(notebookId),
                folderId: String(folder.id),
              },
            }"
            @click="scrollFolderRowIntoView(folder.id)"
          >
            {{ folder.name }}
          </RouterLink>
        </template>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import { ChevronUp } from "lucide-vue-next"
import { computed, onBeforeUnmount, ref, watch } from "vue"
import { RouterLink } from "vue-router"
import { folderIdsWithRowAboveScrollportTop } from "./sidebarActivePathRowsAboveScrollport"

const props = defineProps<{
  pathFolders: Folder[]
  notebookId: number
  scrollRoot: HTMLElement | null
}>()

const clippedIds = ref<number[]>([])

const clippedFolders = computed(() => {
  const idSet = new Set(clippedIds.value)
  return props.pathFolders.filter((f) => idSet.has(f.id))
})

let rafId = 0
let attachedRoot: HTMLElement | null = null
let resizeObserver: ResizeObserver | null = null

function scheduleUpdate() {
  if (rafId !== 0) cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(() => {
    rafId = 0
    updateClipped()
  })
}

function updateClipped() {
  const root = props.scrollRoot
  if (!root || props.pathFolders.length === 0) {
    clippedIds.value = []
    return
  }
  const rootTop = root.getBoundingClientRect().top
  const pathIds = props.pathFolders.map((f) => f.id)
  clippedIds.value = folderIdsWithRowAboveScrollportTop(
    rootTop,
    pathIds,
    (id) => {
      const el = root.querySelector<HTMLElement>(
        `[data-sidebar-folder-row="${id}"]`
      )
      return el?.getBoundingClientRect().top
    }
  )
}

function detachListeners() {
  if (attachedRoot) {
    attachedRoot.removeEventListener("scroll", scheduleUpdate)
    attachedRoot = null
  }
  window.removeEventListener("resize", scheduleUpdate)
  resizeObserver?.disconnect()
  resizeObserver = null
}

function attachListeners() {
  detachListeners()
  const root = props.scrollRoot
  if (!root) {
    clippedIds.value = []
    return
  }
  attachedRoot = root
  root.addEventListener("scroll", scheduleUpdate, { passive: true })
  window.addEventListener("resize", scheduleUpdate, { passive: true })
  resizeObserver = new ResizeObserver(scheduleUpdate)
  resizeObserver.observe(root)
  scheduleUpdate()
}

function scrollFolderRowIntoView(folderId: number) {
  const root = props.scrollRoot
  if (!root) return
  const el = root.querySelector<HTMLElement>(
    `[data-sidebar-folder-row="${folderId}"]`
  )
  el?.scrollIntoView({ block: "nearest", behavior: "smooth" })
}

watch(
  () => props.scrollRoot,
  (root) => {
    if (root) attachListeners()
    else {
      detachListeners()
      clippedIds.value = []
    }
  },
  { immediate: true }
)

watch(
  () => props.pathFolders.map((f) => f.id),
  () => scheduleUpdate(),
  { deep: true }
)

onBeforeUnmount(() => {
  if (rafId !== 0) cancelAnimationFrame(rafId)
  detachListeners()
})
</script>

<style scoped lang="scss">
/* Zero-height sticky wrapper so the hint does not shift tree rows (avoids show/hide flicker). */
.scrollport-path-sticky-anchor {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 0;
  overflow: visible;
  pointer-events: none;
}

.scrollport-path-hint {
  pointer-events: auto;
  background-color: var(--fallback-b1, oklch(var(--b1) / 1));
  border-bottom: 1px solid var(--color-base-300, rgba(0, 0, 0, 0.12));
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.hint-chevron {
  opacity: 0.65;
}

.hint-sep {
  opacity: 0.45;
  padding: 0 0.125rem;
  user-select: none;
}

.hint-link {
  color: inherit;
  text-decoration: none;
  font-weight: 500;
  min-width: 0;

  &:hover {
    text-decoration: underline;
  }
}
</style>

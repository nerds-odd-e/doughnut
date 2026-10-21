<script setup lang="ts">
import { computed, nextTick, ref } from "vue"
import type {
  FolderTrailSegment,
  NotebookRealm,
} from "@generated/donut-backend-api"
import { BookText, ChevronDown } from "@lucide/vue"
import Modal from "@/components/commons/Modal.vue"
import BreadcrumbWithCircle from "./BreadcrumbWithCircle.vue"

const props = defineProps<{
  notebookRealm: NotebookRealm
  ancestorFolders: FolderTrailSegment[]
}>()
const opened = ref(false)
const control = ref<HTMLButtonElement>()
const trail = computed(() =>
  props.ancestorFolders.map((folder) => folder.name).join(" / ")
)
async function close() {
  opened.value = false
  await nextTick()
  control.value?.focus()
}
</script>

<template>
  <button
    ref="control"
    type="button"
    class="notebook-location w-full min-w-0 text-left px-2 py-1"
    aria-label="Open notebook location"
    aria-haspopup="dialog"
    :aria-expanded="opened"
    @click="opened = true"
  >
    <span class="flex items-center gap-1 min-w-0 font-semibold text-sm">
      <BookText class="size-4 shrink-0" aria-hidden="true" />
      <span class="truncate">{{ notebookRealm.notebook.name }}</span>
      <ChevronDown class="size-4 shrink-0 ml-auto" aria-hidden="true" />
    </span>
    <span v-if="trail" class="block truncate text-xs opacity-70">{{ trail }}</span>
  </button>
  <Modal v-if="opened" aria-label="Notebook location" @close_request="close">
    <template #header><h2 class="text-lg font-semibold">Notebook location</h2></template>
    <template #body>
      <nav class="location-path" aria-label="Notebook location">
        <BreadcrumbWithCircle :notebook-realm="notebookRealm" :ancestor-folders="ancestorFolders" />
      </nav>
    </template>
  </Modal>
</template>

<style scoped lang="scss">
.notebook-location {
  min-height: 44px;
}
.location-path :deep(.daisy-breadcrumbs) {
  overflow: visible;
  white-space: normal;
  ul {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
  li {
    min-width: 0;
    &::before { display: none; }
  }
  a {
    min-height: 44px;
    width: 100%;
    white-space: normal;
    overflow-wrap: anywhere;
  }
}
</style>

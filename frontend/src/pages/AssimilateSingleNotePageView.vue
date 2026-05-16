<template>
  <GlobalBar>
    <div class="flex items-center justify-center w-full">
      <span class="text-lg font-semibold">Assimilate Note</span>
    </div>
  </GlobalBar>
  <div class="mx-auto min-w-0 container mt-3">
    <ContentLoader v-if="!note" />
    <Assimilation
      v-if="note"
      v-bind="{ note, ancestorFolders }"
      @assimilation-done="assimilationDone"
      @reload-needed="onReloadNeeded"
      :key="note.id"
    />
  </div>
</template>

<script setup lang="ts">
import type { Folder, Note } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import Assimilation from "@/components/recall/Assimilation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

defineProps({
  note: {
    type: Object as PropType<Note | undefined>,
    required: false,
  },
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
})

const emit = defineEmits<{
  (e: "assimilation-done"): void
  (e: "reload-needed"): void
}>()

const assimilationDone = () => {
  emit("assimilation-done")
}

const onReloadNeeded = () => {
  emit("reload-needed")
}
</script>

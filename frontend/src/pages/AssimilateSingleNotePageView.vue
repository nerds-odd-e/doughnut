<template>
  <GlobalBar>
    <div class="daisy-flex daisy-items-center daisy-justify-center daisy-w-full">
      <span class="daisy-text-lg daisy-font-semibold">Assimilate Note</span>
    </div>
  </GlobalBar>
  <div class="daisy-mx-auto daisy-min-w-0 daisy-container daisy-mt-3">
    <ContentLoader v-if="!note" />
    <Assimilation
      v-if="note"
      v-bind="{ note }"
      @assimilation-done="assimilationDone"
      @reload-needed="onReloadNeeded"
      :key="note.id"
    />
  </div>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import type { PropType } from "vue"
import Assimilation from "@/components/recall/Assimilation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

defineProps({
  note: {
    type: Object as PropType<Note | undefined>,
    required: false,
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

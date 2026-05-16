<template>
  <div v-if="noteTopologies.length > 0" class="flex">
    <div class="w-10 p-0">
      <button
        class="daisy-btn daisy-btn-sm"
        v-if="internalExpandChildren"
        role="button"
        title="collapse children"
        @click="collapse()"
      >
        <ChevronsUpDown class="w-6 h-6 rotate-180" />
      </button>
      <button
        class="daisy-btn daisy-btn-sm"
        v-else
        role="button"
        title="expand children"
        @click="expand()"
      >
        <ChevronsUpDown class="w-6 h-6" />
      </button>
    </div>
    <div class="flex-1">
      <div v-if="!internalExpandChildren">
        <div role="collapsed-children-count">{{ noteTopologies.length }}</div>
      </div>
      <Cards v-else :note-topologies="noteTopologies" :columns="2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import Cards from "./Cards.vue"
import { ChevronsUpDown } from "lucide-vue-next"

const props = defineProps({
  noteTopologies: { type: Array as PropType<NoteTopology[]>, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, required: true },
})

const internalExpandChildren = ref(props.expandChildren)

const collapse = () => {
  internalExpandChildren.value = false
}

const expand = () => {
  internalExpandChildren.value = true
}
</script>

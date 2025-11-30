<template>
  <div v-if="notes.length > 0" class="daisy-flex">
    <div class="daisy-w-10 daisy-p-0">
      <button
        class="daisy-btn daisy-btn-sm"
        v-if="internalExpandChildren"
        role="button"
        title="collapse children"
        @click="collapse()"
      >
        <SvgCollapse />
      </button>
      <button
        class="daisy-btn daisy-btn-sm"
        v-else
        role="button"
        title="expand children"
        @click="expand()"
      >
        <SvgExpand />
      </button>
    </div>
    <div class="daisy-flex-1">
      <div v-if="!internalExpandChildren">
        <div role="collapsed-children-count">{{ notes.length }}</div>
      </div>
      <Cards v-else :note-topologies="notes.map((n) => n.noteTopology)" :columns="2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note } from "@generated/backend"
import Cards from "./Cards.vue"
import SvgCollapse from "../svgs/SvgCollapse.vue"
import SvgExpand from "../svgs/SvgExpand.vue"

const props = defineProps({
  notes: { type: Array as PropType<Note[]>, required: true },
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

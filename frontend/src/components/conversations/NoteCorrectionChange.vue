<template>
  <section data-testid="note-correction-change">
    <h4>Change</h4>
    <p v-if="current === proposed">No change: the wording equals the current content.</p>
    <div v-else class="whitespace-pre-wrap break-words">
      <template v-for="(line, index) in lines" :key="index">
        <div v-if="line.type === 'replaced'"><component :is="MARKS[segment.type].tag" v-for="(segment, at) in line.segments" :key="at" :class="MARKS[segment.type].class">{{ segment.text }}</component></div>
        <component :is="MARKS[line.type].tag" v-else class="block" :class="MARKS[line.type].class">{{ line.text }}<br v-if="!line.text" /></component>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { buildChangedLines } from "@/components/commons/changedLines"

const MARKS = {
  same: { tag: "span", class: "" },
  removed: { tag: "del", class: "bg-error/20" },
  added: { tag: "ins", class: "bg-success/20" },
}

const props = defineProps<{ current: string; proposed: string }>()
const lines = computed(() => buildChangedLines(props.current, props.proposed))
</script>

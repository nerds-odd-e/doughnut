<template>
  <section data-testid="note-correction-change">
    <h4>Change</h4>
    <p v-if="current === proposed">No change: the wording equals the current content.</p>
    <div v-else class="whitespace-pre-wrap break-words">
      <component :is="MARKS[line.type].tag" v-for="(line, index) in lines" :key="index" :class="MARKS[line.type].class">{{ line.text }}<br v-if="!line.text" /></component>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { buildChangedLines } from "@/components/commons/lineDiff"

const MARKS = {
  same: { tag: "div", class: "" },
  removed: { tag: "del", class: "block bg-error/20" },
  added: { tag: "ins", class: "block bg-success/20" },
}

const props = defineProps<{ current: string; proposed: string }>()
const lines = computed(() => buildChangedLines(props.current, props.proposed))
</script>

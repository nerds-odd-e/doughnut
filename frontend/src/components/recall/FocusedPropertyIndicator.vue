<template>
  <p v-if="propertyKey" class="focused-property text-sm font-medium mb-2" data-testid="focused-property-indicator">
    Reviewed property:
    <router-link :to="sourceLocation" class="daisy-link" data-testid="reviewed-property-link">{{ propertyKey }}</router-link>
    <template v-if="propertyValue !== undefined">: {{ propertyValue }}</template>
  </p>
</template>

<script setup lang="ts">
import { computed } from "vue"
import {
  notePropertyLocation,
  notePropertyItemLocation,
} from "@/routes/noteShowLocation"
const props = defineProps<{
  noteId: number
  propertyKey?: string
  propertyValue?: string
}>()
const sourceLocation = computed(() =>
  props.propertyValue === undefined
    ? notePropertyLocation(props.noteId, props.propertyKey!)
    : notePropertyItemLocation(props.noteId, props.propertyKey!, {
        value: props.propertyValue,
        occurrence: 1,
      })
)
</script>

<template>
  <template v-if="suggestedTitle">
    <label>Suggested Title: {{ suggestedTitle }}</label>
    <RadioButtons
      v-model="replaceOrAppendTitle"
      scope-name="titleRadio"
      :options="[
        { value: 'Replace', label: 'Replace title' },
        { value: 'Append', label: 'Append title' },
      ]"
      @update:model-value="updateModelValue"
    />
  </template>
</template>

<script setup lang="ts">
import { ref } from "vue"
import RadioButtons from "../form/RadioButtons.vue"

const props = defineProps<{
  originalTitle: string
  suggestedTitle: string
}>()

const emit = defineEmits<{
  suggestedTitleSelected: [title: string]
}>()

const replaceOrAppendTitle = ref("")

const updateModelValue = () => {
  if (replaceOrAppendTitle.value === "Replace") {
    emit("suggestedTitleSelected", props.suggestedTitle)
  }

  if (replaceOrAppendTitle.value === "Append") {
    const newTitle = `${props.originalTitle} / ${props.suggestedTitle}`
    emit("suggestedTitleSelected", newTitle)
  }
}
</script>

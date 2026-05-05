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
import { NOTE_TITLE_ALTERNATIVE_FORM_JOINER } from "@/utils/noteTitleAlternativeFormJoiner"

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
    const newTitle = `${props.originalTitle}${NOTE_TITLE_ALTERNATIVE_FORM_JOINER}${props.suggestedTitle}`
    emit("suggestedTitleSelected", newTitle)
  }
}
</script>

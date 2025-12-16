<template>
  <RadioButtons
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :scope-name="scopeName"
    :field="field"
    :options="options"
    :error-message="errorMessage"
  >
    <template #labelAddition="{ value }">
      <div class="daisy-text-center">
        <SvgRelationTypeIcon :relation-type="value" :inverse-icon="inverseIcon" />
      </div>
    </template>
  </RadioButtons>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/backend"
// Using string literals for relationType values
import type { PropType } from "vue"
import { computed } from "vue"
import { relationTypeOptions } from "../../models/relationTypeOptions"
import RadioButtons from "../form/RadioButtons.vue"
import SvgRelationTypeIcon from "../svgs/SvgRelationTypeIcon.vue"

const { allowEmpty } = defineProps({
  scopeName: String,
  modelValue: {
    type: String as PropType<NoteTopology["relationType"]>,
    required: true,
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, default: "relationType" },
  inverseIcon: Boolean,
})

defineEmits(["update:modelValue"])

const options = computed(() => {
  const filteredOptions = allowEmpty
    ? relationTypeOptions
    : relationTypeOptions.filter(({ label }) => label !== "no link")

  return filteredOptions.map(({ label }) => ({
    value: label as string,
    label: label as string,
  }))
})
</script>

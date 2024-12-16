<template>
  <RadioButtons
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    v-bind="{ scopeName, field, options, errorMessage }"
  >
    <template #labelAddition="{ value }">
      <div class="daisy-text-center">
        <SvgLinkTypeIcon :link-type="value" :inverse-icon="inverseIcon" />
      </div>
    </template>
  </RadioButtons>
</template>

<script setup lang="ts">
import { NoteTopology } from "@/generated/backend"
import type { PropType } from "vue"
import { computed } from "vue"
import { linkTypeOptions } from "../../models/linkTypeOptions"
import RadioButtons from "../form/RadioButtons.vue"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"

const { allowEmpty } = defineProps({
  scopeName: String,
  modelValue: {
    type: String as PropType<NoteTopology.linkType>,
    required: true,
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, default: "linkType" },
  inverseIcon: Boolean,
})

defineEmits(["update:modelValue"])

const options = computed(() => {
  const filteredOptions = allowEmpty
    ? linkTypeOptions
    : linkTypeOptions.filter(({ label }) => label !== "no link")

  return filteredOptions.map(({ label }) => ({
    value: label,
    label,
  }))
})
</script>

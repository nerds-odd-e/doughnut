<template>
  <InputWithType v-bind="{ scopeName, field: '', errorMessage, title: titlized }">
    <PopButton :aria-label="titlized">
      <template #button_face>
        <SvgLinkTypeIcon :link-type="modelValue" :inverse-icon="inverseIcon" />
        {{ label }}
      </template>
      <!-- prettier-ignore -->
      <template #default="{ closer }">
        <LinkTypeSelect
          v-bind="{
            scopeName,
            modelValue,
            errorMessage,
            allowEmpty,
            field,
            inverseIcon,
          }"
          @update:model-value="
            closer();
            $emit('update:modelValue', $event);
          "
        />
      </template>
    </PopButton>
  </InputWithType>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/backend"
// Using string literals for linkType values
import { camelCase, startCase } from "es-toolkit"
import type { PropType } from "vue"
import { computed } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import InputWithType from "../form/InputWithType.vue"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
import LinkTypeSelect from "./LinkTypeSelect.vue"

const props = defineProps({
  scopeName: String,
  modelValue: {
    type: String as PropType<NoteTopology["linkType"]>,
    default: () => "no link" as NoteTopology["linkType"],
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, default: "linkType" },
  inverseIcon: Boolean,
})

defineEmits(["update:modelValue"])

const titlized = computed(() => startCase(camelCase(props.field ?? "")))
const label = computed(() => props.modelValue || "default")
</script>

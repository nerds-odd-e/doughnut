<template>
  <InputWithType v-bind="{ scopeName, field: '', errorMessage, title: titlized }">
    <PopButton :aria-label="titlized">
      <template #button_face>
        <SvgRelationTypeIcon
          v-if="modelValue"
          :relation-type="modelValue"
          :inverse-icon="inverseIcon"
        />
        {{ label }}
      </template>
      <!-- prettier-ignore -->
      <template #default="{ closer }">
        <RelationTypeSelect
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
// Using string literals for relationType values
import { camelCase, startCase } from "es-toolkit"
import type { PropType } from "vue"
import { computed } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import InputWithType from "../form/InputWithType.vue"
import SvgRelationTypeIcon from "../svgs/SvgRelationTypeIcon.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"

const props = defineProps({
  scopeName: String,
  modelValue: {
    type: String as PropType<NoteTopology["relationType"]>,
    default: undefined,
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, default: "relationType" },
  inverseIcon: Boolean,
})

defineEmits(["update:modelValue"])

const titlized = computed(() => startCase(camelCase(props.field ?? "")))
const label = computed(() => props.modelValue || "default")
</script>

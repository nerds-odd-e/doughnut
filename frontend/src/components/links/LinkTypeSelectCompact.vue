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
            modelValue: modelValue as string as NoteTopic.linkType,
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
import { NoteCreationDTO, NoteTopic } from "@/generated/backend"
import { camelCase, startCase } from "lodash"
import { PropType, computed } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import InputWithType from "../form/InputWithType.vue"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
import LinkTypeSelect from "./LinkTypeSelect.vue"

const { modelValue, field } = defineProps({
  scopeName: String,
  modelValue: {
    type: String as PropType<NoteCreationDTO.linkTypeToParent>,
    default: () => "no link" as NoteTopic.linkType,
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, defalt: "linkType" },
  inverseIcon: Boolean,
})

defineEmits(["update:modelValue"])

const titlized = computed(() => startCase(camelCase(field)))
const label = computed(() => modelValue || "default")
</script>

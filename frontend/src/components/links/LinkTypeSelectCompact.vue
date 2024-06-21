<template>
  <InputWithType v-bind="{ scopeName, field: '', errors, title: titlized }">
    <PopButton :aria-label="titlized">
      <template #button_face>
        <SvgLinkTypeIcon :link-type="modelValue" :inverse-icon="inverseIcon" />
        {{ label }}
      </template>
      <template #default="{ closer }">
        <LinkTypeSelect
          v-bind="{
            scopeName,
            modelValue: modelValue as string as NoteTopic.linkType,
            errors,
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

<script lang="ts">
import { NoteCreationDTO, NoteTopic } from "@/generated/backend"
import { camelCase, startCase } from "lodash"
import { PropType, defineComponent } from "vue"
import PopButton from "../commons/Popups/PopButton.vue"
import InputWithType from "../form/InputWithType.vue"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
import LinkTypeSelect from "./LinkTypeSelect.vue"

export default defineComponent({
  props: {
    scopeName: String,
    modelValue: {
      type: String as PropType<NoteCreationDTO.linkTypeToParent>,
      default: () => "no link" as NoteTopic.linkType,
    },
    errors: String,
    allowEmpty: { type: Boolean, default: false },
    field: { type: String, defalt: "linkType" },
    inverseIcon: Boolean,
  },
  components: { PopButton, SvgLinkTypeIcon, LinkTypeSelect, InputWithType },
  emits: ["update:modelValue"],
  computed: {
    titlized() {
      return startCase(camelCase(this.field))
    },
    label() {
      return this.modelValue ? this.modelValue : "default"
    },
  },
})
</script>

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
            modelValue,
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
import { defineComponent, PropType } from "vue";
import { startCase, camelCase } from "lodash";
import PopButton from "../commons/Popups/PopButton.vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import InputWithType from "../form/InputWithType.vue";

export default defineComponent({
  props: {
    scopeName: String,
    modelValue: {
      type: String as PropType<Generated.LinkType>,
      default: () => "no link" as Generated.LinkType,
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
      return startCase(camelCase(this.field));
    },
    label() {
      return this.modelValue ? this.modelValue : "default";
    },
  },
});
</script>

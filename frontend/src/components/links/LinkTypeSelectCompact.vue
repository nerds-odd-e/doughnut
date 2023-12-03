<template>
  <InputWithType v-bind="{ scopeName, field: '', errors, title: titlized }">
    <PopButton>
      <template #button_face>
        <SvgLinkTypeIcon
          :aria-label="titlized"
          :link-type="modelValue"
          :inverse-icon="inverseIcon"
        />
        {{ label }}
      </template>
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
          popup.done($event);
          $emit('update:modelValue', $event);
        "
      />
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
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  setup() {
    return { ...asPopup() };
  },
  props: {
    scopeName: String,
    modelValue: {
      type: String as PropType<Generated.LinkType>,
      required: true,
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

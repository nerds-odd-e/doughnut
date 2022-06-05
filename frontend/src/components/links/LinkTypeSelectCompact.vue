<template>
  <InputWithType v-bind="{ scopeName, field: '', errors, title: titlized }">
    <PopupButton :aria-label="titlized">
      <template #button_face>
        <SvgLinkTypeIcon :link-type="modelValue" :inverse-icon="inverseIcon" />
        {{ label }}
      </template>
      <template #dialog_body="{ doneHandler }">
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
            doneHandler($event);
            $emit('update:modelValue', $event);
          "
        />
      </template>
    </PopupButton>
  </InputWithType>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { startCase, camelCase } from "lodash";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import InputWithType from "../form/InputWithType.vue";

export default defineComponent({
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
  components: { PopupButton, SvgLinkTypeIcon, LinkTypeSelect, InputWithType },
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

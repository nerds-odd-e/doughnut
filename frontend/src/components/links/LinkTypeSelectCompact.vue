<template>
  <InputWithType v-bind="{ scopeName, field: '', errors, title: titlized }">
    <PopupButton :aria-label="titlized">
      <template #button_face>
        <SvgLinkTypeIcon
          :link-type-id="modelValue"
          :inverse-icon="inverseIcon"
        />
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

<script>
import { startCase, camelCase } from "lodash";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import InputWithType from "../form/InputWithType.vue";
import { linkTypeOptions } from "../../models/linkTypeOptions";

export default {
  props: {
    scopeName: String,
    modelValue: Object,
    errors: Object,
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
      const linkType = linkTypeOptions.find(
        (lt) => lt.value.toString() === this.modelValue
      );
      return linkType ? linkType.label : "default";
    },
  },
};
</script>

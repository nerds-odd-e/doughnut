<template>

  <RadioButtons
    v-model="modelValue"
    @update:modelValue="$emit('update:modelValue', $event)"
    v-bind="{ scopeName, field, options, errors }"
  >
    <template #labelAddition="{ value }">
      <div class="text-center">
        <SvgLinkTypeIcon :linkTypeId="value" :inverseIcon="inverseIcon" />
      </div>
    </template>
  </RadioButtons>

</template>

<script>
import RadioButtons from "../form/RadioButtons.vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import { linkTypeOptions } from "../../models/linkTypeOptions"

export default {
  name: "LinkTypeSelect",
  props: {
    scopeName: String,
    modelValue: Object,
    errors: Object,
    allowEmpty: { type: Boolean, default: false },
    field: { type: String, defalt: "linkType" },
    inverseIcon: Boolean,
  },
  components: { RadioButtons, SvgLinkTypeIcon },
  emits: ["update:modelValue"],
  computed: {
    options() {
      if (!!this.allowEmpty) {
        return [
          { value: 0, label: "Default" },
          ...linkTypeOptions,
        ];
      }
      return linkTypeOptions;
    },
  },
};
</script>

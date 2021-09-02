<template>
  <RadioButtons
    v-if="!!$staticInfo"
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
          { value: "", label: "Default" },
          ...this.$staticInfo.linkTypeOptions,
        ];
      }
      return this.$staticInfo.linkTypeOptions;
    },
  },
};
</script>

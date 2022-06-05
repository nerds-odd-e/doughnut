<template>
  <RadioButtons
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    v-bind="{ scopeName, field, options, errors }"
  >
    <template #labelAddition="{ value }">
      <div class="text-center">
        <SvgLinkTypeIcon :link-type-name="value" :inverse-icon="inverseIcon" />
      </div>
    </template>
  </RadioButtons>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import RadioButtons from "../form/RadioButtons.vue";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import { linkTypeOptions } from "../../models/linkTypeOptions";

export default defineComponent({
  name: "LinkTypeSelect",
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
  components: { RadioButtons, SvgLinkTypeIcon },
  emits: ["update:modelValue"],
  computed: {
    options() {
      return this.optionsRaw.map(({ label }) => ({
        value: label,
        label,
      }));
    },
    optionsRaw() {
      if (this.allowEmpty) {
        return linkTypeOptions;
      }
      return linkTypeOptions.filter(({ label }) => label !== "no link");
    },
  },
});
</script>

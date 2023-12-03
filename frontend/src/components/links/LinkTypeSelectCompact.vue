<template>
  <InputWithType v-bind="{ scopeName, field: '', errors, title: titlized }">
    <PopButton1 :aria-label="titlized">
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
    </PopButton1>
  </InputWithType>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { startCase, camelCase } from "lodash";
import PopButton1 from "../commons/Popups/PopButton1.vue";
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
  components: { PopButton1, SvgLinkTypeIcon, LinkTypeSelect, InputWithType },
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

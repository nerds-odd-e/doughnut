<template>
  <RadioButtons
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    v-bind="{ scopeName, field, options }"
    :errors="errorsObject"
  >
    <template #labelAddition="{ value }">
      <div class="text-center">
        <SvgLinkTypeIcon :link-type="value" :inverse-icon="inverseIcon" />
      </div>
    </template>
  </RadioButtons>
</template>

<script lang="ts">
import { NoteTopic } from "@/generated/backend"
import { PropType, defineComponent, computed } from "vue"
import { linkTypeOptions } from "../../models/linkTypeOptions"
import RadioButtons from "../form/RadioButtons.vue"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"

export default defineComponent({
  name: "LinkTypeSelect",
  props: {
    scopeName: String,
    modelValue: {
      type: String as PropType<NoteTopic.linkType>,
      required: true,
    },
    errors: String,
    allowEmpty: { type: Boolean, default: false },
    field: { type: String, default: "linkType" },
    inverseIcon: Boolean,
  },
  components: { RadioButtons, SvgLinkTypeIcon },
  emits: ["update:modelValue"],
  setup(props) {
    const errorsObject = computed(() =>
      props.errors ? { [props.field || "linkType"]: props.errors } : undefined
    )

    const options = computed(() => {
      const filteredOptions = props.allowEmpty
        ? linkTypeOptions
        : linkTypeOptions.filter(({ label }) => label !== "no link")

      return filteredOptions.map(({ label }) => ({
        value: label,
        label,
      }))
    })

    return {
      errorsObject,
      options,
    }
  },
})
</script>
<template>
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="modelValue.level"
    :errors="errors.level"
    :options="
      [0, 1, 2, 3, 4, 5, 6].map((level) => ({
        value: level,
        label: level,
      }))
    "
    @update:model-value="updateModelValue({ level: Number.parseInt($event) })"
  />

  <CheckInput
    scope-name="review_setting"
    field="rememberSpelling"
    :model-value="modelValue.rememberSpelling"
    :errors="errors.rememberSpelling"
    @update:model-value="updateModelValue({ rememberSpelling: $event })"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import CheckInput from "../form/CheckInput.vue";
import RadioButtons from "../form/RadioButtons.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: true });
  },
  props: {
    noteId: { type: Number, required: true },
    modelValue: {
      type: Object as PropType<Omit<Generated.ReviewSetting, "id">>,
      required: true,
    },
    errors: Object,
  },
  components: { CheckInput, RadioButtons },
  emits: ["update:modelValue"],
  methods: {
    updateModelValue(newValue: Partial<Generated.ReviewSetting>) {
      const updated = {
        ...this.modelValue,
        ...newValue,
      };
      this.$emit("update:modelValue", updated);
      this.api.reviewMethods.updateReviewSetting(this.noteId, updated);
    },
  },
});
</script>

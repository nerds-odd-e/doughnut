<template>
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="formData.level"
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
    :model-value="formData.rememberSpelling"
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
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    reviewSetting: {
      type: Object as PropType<Generated.ReviewSetting>,
      required: false,
    },
  },
  components: { CheckInput, RadioButtons },
  emits: ["levelChanged"],
  data() {
    return {
      formData: (this.reviewSetting ? this.reviewSetting : {}) as Omit<
        Generated.ReviewSetting,
        "id"
      >,
      errors: {} as Partial<Generated.ReviewSetting>,
    };
  },
  methods: {
    updateModelValue(newValue: Partial<Generated.ReviewSetting>) {
      this.formData = {
        ...this.formData,
        ...newValue,
      };
      this.api.reviewMethods
        .updateReviewSetting(this.noteId, this.formData)
        .then(() => {
          if (newValue.level !== undefined) {
            this.$emit("levelChanged", newValue.level);
          }
        })
        .catch((error) => (this.errors = error));
    },
  },
});
</script>

<template>
  {{ formData }}
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="formData.level"
    :errors="formErrors.level"
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
    :errors="formErrors.rememberSpelling"
    @update:model-value="updateModelValue({ rememberSpelling: $event })"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import CheckInput from "../form/CheckInput.vue";
import RadioButtons from "../form/RadioButtons.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: true });
  },
  props: {
    noteId: { type: Number, required: true },
  },
  components: { CheckInput, RadioButtons },
  emits: ["levelChanged"],
  data() {
    return {
      formData: {} as Omit<Generated.ReviewSetting, "id">,
      formErrors: {} as Partial<Generated.ReviewSetting>,
    };
  },
  methods: {
    fetchData() {
      this.api.reviewMethods.getReviewSetting(this.noteId).then((res) => {
        this.formData = res;
      });
    },
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
        });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

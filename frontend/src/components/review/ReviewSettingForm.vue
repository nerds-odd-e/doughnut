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
  <CheckInput
    scope-name="review_setting"
    field="skipReview"
    :model-value="formData.skipReview"
    :errors="errors.skipReview"
    @update:model-value="updateModelValue({ skipReview: $event })"
  />
</template>

<script lang="ts">
import { ReviewSetting } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent } from "vue"
import CheckInput from "../form/CheckInput.vue"
import RadioButtons from "../form/RadioButtons.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    noteId: { type: Number, required: true },
    reviewSetting: {
      type: Object as PropType<ReviewSetting>,
      required: false,
    },
  },
  components: { CheckInput, RadioButtons },
  emits: ["levelChanged"],
  data() {
    return {
      formData: (this.reviewSetting ? this.reviewSetting : {}) as ReviewSetting,
      errors: {} as Partial<ReviewSetting>,
    }
  },
  methods: {
    updateModelValue(newValue: ReviewSetting) {
      this.formData = {
        ...this.formData,
        ...newValue,
      }
      this.managedApi.restNoteController
        .updateReviewSetting(this.noteId, this.formData)
        .then(() => {
          if (newValue.level !== undefined) {
            this.$emit("levelChanged", newValue.level)
          }
        })
        .catch((error) => (this.errors = error))
    },
  },
})
</script>

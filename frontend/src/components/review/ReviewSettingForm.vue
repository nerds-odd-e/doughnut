<template>
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="levelAsString"
    :error-message="errors.level"
    :options="levelOptions"
    @update:model-value="updateLevel"
  />
  <CheckInput
    scope-name="review_setting"
    field="rememberSpelling"
    :model-value="formData.rememberSpelling"
    :error-message="errors.rememberSpelling"
    @update:model-value="updateModelValue({ rememberSpelling: $event })"
  />
  <CheckInput
    scope-name="review_setting"
    field="skipReview"
    :model-value="formData.skipReview"
    :error-message="errors.skipReview"
    @update:model-value="updateModelValue({ skipReview: $event })"
  />
</template>

<script lang="ts">
import { ReviewSetting } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent, computed, ref } from "vue"
import CheckInput from "../form/CheckInput.vue"
import RadioButtons from "../form/RadioButtons.vue"

export default defineComponent({
  components: { CheckInput, RadioButtons },
  props: {
    noteId: { type: Number, required: true },
    reviewSetting: {
      type: Object as PropType<ReviewSetting>,
      required: false,
    },
  },
  emits: ["levelChanged"],
  setup(props, { emit }) {
    const { managedApi } = useLoadingApi()

    const formData = ref<ReviewSetting>(props.reviewSetting || {})
    const errors = ref<Partial<Record<keyof ReviewSetting, string>>>({})

    const levelAsString = computed(() =>
      formData.value.level !== undefined
        ? formData.value.level.toString()
        : undefined
    )

    const levelOptions = [0, 1, 2, 3, 4, 5, 6].map((level) => ({
      value: level.toString(),
      label: level.toString(),
    }))

    const updateModelValue = (newValue: Partial<ReviewSetting>) => {
      formData.value = {
        ...formData.value,
        ...newValue,
      }
      managedApi.restNoteController
        .updateReviewSetting(props.noteId, formData.value)
        .then(() => {
          if (newValue.level !== undefined) {
            emit("levelChanged", newValue.level)
          }
        })
        .catch((error) => {
          errors.value = error
        })
    }

    const updateLevel = (value: string) => {
      updateModelValue({ level: Number.parseInt(value) })
    }

    return {
      formData,
      errors,
      levelAsString,
      levelOptions,
      updateModelValue,
      updateLevel,
    }
  },
})
</script>

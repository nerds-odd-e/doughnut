<template>
  <TextInput
    scope-name="note"
    field="url"
    :model-value="modelValue.url"
    :errors="errors.url"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, url: $event })
    "
  />
</template>

<script lang="ts">
import { NoteAccessoriesDTO } from "@/generated/backend"
import { PropType, defineComponent } from "vue"
import TextInput from "../../form/TextInput.vue"

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<NoteAccessoriesDTO>,
      required: true,
    },
    errors: {
      type: Object,
      default() {
        return {}
      },
    },
  },
  emits: ["update:modelValue"],
  components: { TextInput },
})
</script>

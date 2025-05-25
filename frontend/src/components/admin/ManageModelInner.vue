<template>
  <div class="daisy-container">
    <div>
      <label
        >Question Generation
        <Select
          :options="modelList"
          v-model="localSelectedModels.questionGenerationModel"
          scope-name="Question Generation"
          field=""
        ></Select>
      </label>
    </div>
    <div>
      <label
        >Evaluation
        <Select
          :options="modelList"
          v-model="localSelectedModels.evaluationModel"
          scope-name="Evaluation"
          field=""
        ></Select
      ></label>
    </div>
    <div>
      <label
        >Others
        <Select
          :options="modelList"
          v-model="localSelectedModels.othersModel"
          scope-name="Others"
          field=""
        ></Select
      ></label>
    </div>
    <div>
      <button @click="$emit('save', localSelectedModels)">Save</button>
    </div>
  </div>
</template>
<script lang="ts" setup>
import type { PropType } from "vue"
import { ref } from "vue"
import type { GlobalAiModelSettings } from "generated/backend"
import Select from "../form/Select.vue"

const props = defineProps({
  modelList: {
    type: Object as PropType<string[]>,
    required: true,
  },
  selectedModels: {
    type: Object as PropType<GlobalAiModelSettings>,
    required: true,
  },
})

defineEmits(["save"])

const localSelectedModels = ref(props.selectedModels)
</script>

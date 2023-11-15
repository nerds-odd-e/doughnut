<template>
  <div class="container">
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
import { PropType, ref } from "vue";
import Select from "../form/Select.vue";

const props = defineProps({
  modelList: {
    type: Object as PropType<string[]>,
    required: true,
  },
  selectedModels: {
    type: Object as PropType<Generated.GlobalAiModelSettings>,
    required: true,
  },
});

const emit = defineEmits(["save"]);

const localSelectedModels = ref(props.selectedModels);
</script>

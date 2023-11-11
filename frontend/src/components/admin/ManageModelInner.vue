<template>
  <div class="container">
    <div>
      <label
        >Question Generation
        <DropdownList
          :options="modelList"
          :default-option="selectedModels.currentQuestionGenerationModelVersion"
          scope-name="Question Generation"
          field=""
        ></DropdownList>
      </label>
    </div>
    <div>
      <label
        >Evaluation
        <DropdownList
          :options="modelList"
          :default-option="selectedModels.currentEvaluationModelVersion"
          scope-name="Evaluation"
          field=""
        ></DropdownList
      ></label>
    </div>
    <div>
      <label
        >Others
        <DropdownList
          :options="modelList"
          :default-option="selectedModels.currentOthersModelVersion"
          scope-name="Others"
          field=""
        ></DropdownList
      ></label>
    </div>
    <div>
      <button @click="save()">Save</button>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { PropType } from "vue";
import DropdownList from "../form/Select.vue";

const { modelList, selectedModels } = defineProps({
  modelList: {
    type: Object as PropType<Generated.ModelVersionOption[]>,
    required: true,
  },
  selectedModels: {
    type: Object as PropType<Generated.CurrentModelVersionResponse>,
    required: true,
  },
});

function save() {}
</script>

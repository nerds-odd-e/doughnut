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
      <button @click="save()">Save</button>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { PropType, ref } from "vue";
import Select from "../form/Select.vue";

const props = defineProps({
  modelList: {
    type: Object as PropType<Generated.ModelVersionOption[]>,
    required: true,
  },
  selectedModels: {
    type: Object as PropType<Generated.GlobalAiModelSettings>,
    required: true,
  },
});

const localSelectedModels = ref(props.selectedModels);

function save() {}
</script>

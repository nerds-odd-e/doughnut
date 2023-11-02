<template>
  <div class="manage-model">
    <div class="model-section">
      <div class="model-title">Question Generation</div>
      <div>
        <DropdownList
          :options="getOptionList('Question Generation')"
          :default-option="getDefaultSelected('Question Generation')"
          scope-name="Question Generation"
          field=""
          class="model-option"
          :onchange="selectOption"
        ></DropdownList>
      </div>
    </div>
    <div class="model-section">
      <div class="model-title">Evaluation</div>
      <div>
        <DropdownList
          :options="getOptionList('Evaluation')"
          :default-option="getDefaultSelected('Evaluation')"
          scope-name="Evaluation"
          field=""
          class="model-option"
          :onchange="selectOption"
        ></DropdownList>
      </div>
    </div>
    <div class="model-section">
      <div class="model-title">Others</div>
      <div>
        <DropdownList
          :options="getOptionList('Others')"
          :default-option="getDefaultSelected('Others')"
          scope-name="Others"
          field=""
          class="model-option"
          :onchange="selectOption"
        ></DropdownList>
      </div>
    </div>
    <div class="btnContainer">
      <button id="saveBtn" class="saveBtn" @click="save()">Save</button>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue";
import DropdownList from "../form/Select.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

const { api } = useLoadingApi();
const trainingList = ref([]);

onMounted(() => {
  Promise.all([api.ai.getManageModel(), api.ai.getManageModelSelected()]).then(
    (results) => {
      const [modelListRes, selectedModelRes] = results;
      const modelList = [{ label: "---" }, ...modelListRes];

      const trainingListTmp = [];
      trainingListTmp.push({
        list: modelList,
        selected: selectedModelRes.currentQuestionGenerationModelVersion,
        training_engine: "Question Generation",
      });
      trainingListTmp.push({
        list: modelList,
        selected: selectedModelRes.currentEvaluationModelVersion,
        training_engine: "Evaluation",
      });
      trainingListTmp.push({
        list: modelList,
        selected: selectedModelRes.currentOthersModelVersion,
        training_engine: "Others",
      });
      trainingList.value = trainingListTmp;
    },
  );
});

function selectOption(k, v) {
  trainingList.value.forEach((t) => {
    if (t.training_engine === k) t.selected = v;
  });
}

function getOptionList(trainingEngine) {
  return trainingList.value.find((t) => t.training_engine === trainingEngine)
    ?.list;
}

function getDefaultSelected(trainingEngine) {
  return trainingList.value.find((t) => t.training_engine === trainingEngine)
    ?.selected;
}

function save() {}
</script>

<style scoped>
.manage-model {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 540px;
}
.model-title {
  width: 200px;
  padding-left: 18px;
}
.model-section {
  display: flex;
  align-items: center;
  gap: 18px;
  width: 600px;
}

.model-option {
  min-width: 300px;
}

.btnContainer {
  position: relative;
  margin-top: 3px;
}

.saveBtn {
  position: absolute;
  right: 0;
}
</style>

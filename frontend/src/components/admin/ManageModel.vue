<template>
  <div class="manage-model">
    <div class="model-section">
      <div class="model-title">Question Generation</div>
      <div>
        <DropdownList
          :options="getOptionList('Question Generation')"
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
const selectionList = ref([]);

onMounted(() => {
  api.ai.getManageModel().then((res) => (selectionList.value = res));
});

function selectOption(k, v) {
  selectionList.value.forEach((selectO) => {
    if (selectO.training_engine === k) selectO.selected = v;
  });
  // eslint-disable-next-line no-console
  console.log(selectionList);
}

function getOptionList(trainingEngine) {
  return selectionList.value.find(
    (selectionO) => selectionO.training_engine === trainingEngine,
  )?.list;
}

function save() {}
</script>

<style scoped>
.manage-model {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 500px;
}
.model-title {
  width: 200px;
  padding-left: 18px;
}
.model-section {
  display: flex;
  align-items: center;
  gap: 18px;
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

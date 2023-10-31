<template>
  <div class="manage-model">
    <div class="model-section">
      <div class="model-title">Question Generation</div>
      <div>
        <DropdownList
          :options="selectionList"
          scope-name="modelmgmt"
          field=""
          class="model-option"
        ></DropdownList>
      </div>
    </div>
    <div class="model-section">
      <div class="model-title">Evaluation</div>
      <div>
        <DropdownList
          :options="selectionList"
          scope-name="modelmgmt"
          field=""
          class="model-option"
        ></DropdownList>
      </div>
    </div>
    <div class="model-section">
      <div class="model-title">Other</div>
      <div>
        <DropdownList
          :options="selectionList"
          scope-name="modelmgmt"
          field=""
          class="model-option"
        ></DropdownList>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import DropdownList from "../form/Select.vue";

const selectionList = [
  {
    key: "test_key",
    value: "test_value",
    label: "model1",
  },
  {
    key: "test_key2",
    value: "test_value2",
    label: "model2",
  },
];
</script>
<style scoped>
.manage-model {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
</style>

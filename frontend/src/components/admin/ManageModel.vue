<template>
  <div>
    <div>Question Generation</div>
    <div>
      <DropdownList
        :options="selectionList"
        scope-name="modelmgmt"
        field=""
      ></DropdownList>
    </div>
  </div>
</template>
<script setup lang="ts">
import DropdownList from "../form/Select.vue";

const selectionList = [
  {
    key: "test_key",
    value: "test_value",
    label: "model 777",
  },
  {
    key: "test_key2",
    value: "test_value2",
    label: "model888",
  },
];
</script>

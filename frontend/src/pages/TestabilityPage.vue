<template>
  <div class="container">
    <h1>Testability</h1>
    <CheckInput
      scopeName="testability"
      v-model="featureToggle"
      field="featureToggle"
    />
    <TextInput
      scopeName="testability"
      v-model="randomSelector"
      field="randomSelector"
      hint="can be 'first' or 'last'"
      @blur="updateRandomSelector"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import CheckInput from "../components/form/CheckInput.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import TextInput from "../components/form/TextInput.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  data() {
    return {
      featureToggle: false,
      randomSelector: "",
    };
  },
  components: { CheckInput, TextInput },
  methods: {
    updateRandomSelector() {
      this.storedApi.testability.setRandomizer(this.randomSelector);
    },
  },
  watch: {
    featureToggle() {
      this.storedApi.testability.setFeatureToggle(this.featureToggle);
    },
  },
});
</script>

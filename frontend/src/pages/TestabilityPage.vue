<template>
    <div class="container">
      <h1>Testability</h1>
      <CheckInput
        scopeName="testability"
        v-model="featureToggle"
        field="featureToggle"
      />
    </div>
</template>

<script>
import CheckInput from "../components/form/CheckInput.vue";
import storedApi from  '../managedApi/storedApi';

export default {
  data() {
    return {
      featureToggle: null
    }

  },
  components: { CheckInput },
  watch: {
    featureToggle() {
      storedApi(this).setFeatureToggle(this.featureToggle)
    }

  }
}
</script>

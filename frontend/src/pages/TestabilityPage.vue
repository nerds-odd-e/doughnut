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
import storedComponent from "../store/storedComponent";

export default storedComponent({
  data() {
    return {
      featureToggle: null
    }

  },
  components: { CheckInput },
  watch: {
    featureToggle() {
      this.storedApi().setFeatureToggle(this.featureToggle)
    }

  }
})
</script>

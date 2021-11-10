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
import { storedApiSetFeatureToggle } from '../storedApi';

export default {
  data() {
    return {
      featureToggle: null
    }

  },
  components: { CheckInput },
  watch: {
    featureToggle() {
      storedApiSetFeatureToggle(this.$store, this.featureToggle)
    }

  }
}
</script>

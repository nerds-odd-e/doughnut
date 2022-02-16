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
import { storedApi } from '../storedApi';

export default {
  data() {
    return {
      featureToggle: null
    }

  },
  components: { CheckInput },
  watch: {
    featureToggle() {
      storedApi(this.$store).setFeatureToggle(this.featureToggle)
    }

  }
}
</script>

<template>
  <h3>Unable to generate offline assessment</h3>
  <br />
  <p>{{ errorMessage }}</p>
  <template v-if="!isLoggedIn">
    <div class="login-container">
      <p>You can log in here:</p>
      <LoginButton />
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    isLoggedIn: Boolean,
    errorMessage: String,
  },
});
</script>
<style scoped>
.login-container {
  display: flex;
  align-items: center;
}
</style>

<template>
  <svg class="doughnut-ring" viewBox="-50 -50 100 100" width="100" height="100">
    <SvgLogin
      role="button"
      aria-label="Login via Github"
      class="login"
      @click="login"
      width="50px"
      height="50px"
      y="-25"
      x="-25"
    />
  </svg>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import loginOrRegisterAndHaltThisThread from "../../managedApi/window/loginOrRegisterAndHaltThisThread";
import SvgLogin from "../svgs/SvgLogin.vue";

export default defineComponent({
  methods: {
    login() {
      loginOrRegisterAndHaltThisThread();
    },
  },
  components: { SvgLogin },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  pointer-events: none;
  font-size: 0.8rem;
  font-weight: bold;
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}
.doughnut-ring-pieces {
  pointer-events: visiblePainted;
  transform: rotate(45deg);
}

.login {
  pointer-events: visiblePainted;
  top: -20px;
}
</style>

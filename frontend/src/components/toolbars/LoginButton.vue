<template>
  <SvgLogin
    role="button"
    aria-label="Login via Github"
    class="login"
    @click="login"
    width="30px"
    height="30px"
  />
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

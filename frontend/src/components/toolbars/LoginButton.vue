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
import SvgLogin from "@/components/svgs/SvgLogin.vue"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { defineComponent } from "vue"

export default defineComponent({
  methods: {
    login() {
      loginOrRegisterAndHaltThisThread()
    },
  },
  components: { SvgLogin },
})
</script>

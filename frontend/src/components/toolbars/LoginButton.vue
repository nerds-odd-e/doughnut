<template>
  <button
    type="button"
    aria-label="Login via Github"
    class="login cursor-pointer"
    @click="login"
  >
    <KeyRound :size="30" />
  </button>
</template>

<script lang="ts">
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { KeyRound } from "@lucide/vue"
import { defineComponent } from "vue"

export default defineComponent({
  components: { KeyRound },
  methods: {
    login() {
      loginOrRegisterAndHaltThisThread()
    },
  },
})
</script>

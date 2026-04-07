<template>
  <KeyRound
    role="button"
    aria-label="Login via Github"
    class="login cursor-pointer"
    :size="30"
    @click="login"
  />
</template>

<script lang="ts">
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { KeyRound } from "lucide-vue-next"
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

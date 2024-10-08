<template>
  <SvgAgree
    role="button"
    aria-label="Login"
    class="login"
    @click="aggree"
    width="30px"
    height="30px"
  />
</template>

<script lang="ts">
import SvgAgree from "@/components/svgs/SvgAgree.vue"
import { defineComponent } from "vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

export default defineComponent({
  methods: {
    aggree() {
      popups.alert("Agree Success")
    },
  },
  components: { SvgAgree },
})
</script>

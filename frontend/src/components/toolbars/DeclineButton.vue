<template>
  <SvgDecline
    role="button"
    aria-label="Decline"
    class="decline"
    @click="decline"
    width="30px"
    height="30px"
  />
</template>

<script lang="ts">
import SvgDecline from "@/components/svgs/SvgDecline.vue"
import { defineComponent } from "vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

export default defineComponent({
  methods: {
    decline() {
      popups.alert("Decline Success")
    },
  },
  components: { SvgDecline },
})
</script>

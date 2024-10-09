<template>
  <SvgAgree
    role="button"
    aria-label="Agree"
    class="agree"
    @click="agree"
    width="30px"
    height="30px"
  />
</template>

<script lang="ts">
import SvgAgree from "@/components/svgs/SvgAgree.vue"
import { defineComponent } from "vue"
// import usePopups from "../commons/Popups/usePopups"
import useLoadingApi from "@/managedApi/useLoadingApi"

// const { popups } = usePopups()

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
  },
  props: {
    conversation: {
      type: Object,
      required: true,
    },
  },
  methods: {
    // agree() {
    //   popups.alert(`Agree Success for conversation ID: ${this.conversation.id}`)
    // },
    async agree() {
      if (this.conversation.id === undefined) {
        return
      }
      await this.managedApi.restReviewPointController.markAsRepeated(
        this.conversation.id,
        true
      )
      // this.$emit("reviewed")
    },
  },
  components: { SvgAgree },
})
</script>

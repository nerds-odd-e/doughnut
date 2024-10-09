<template>
  <div>
    <SvgAgree
      role="button"
      aria-label="Agree"
      class="agree"
      @click="agree"
      width="30px"
      height="30px"
      data-testid="AgreeButton"
    />
    <SvgDecline
      role="button"
      aria-label="Decline"
      class="decline"
      @click="agree"
      width="30px"
      height="30px"
      data-testid="DeclineButton"
    />
  </div>
</template>

<script lang="ts">
import SvgAgree from "@/components/svgs/SvgAgree.vue"
import SvgDecline from "@/components/svgs/SvgDecline.vue"
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
      id: Number,
      type: Object,
      required: true,
    },
  },
  emits: ["updated"],
  methods: {
    // agree() {
    //   popups.alert(`Agree Success for conversation ID: ${this.conversation.id}`)
    // },
    async agree() {
      if (this.conversation.id === undefined) {
        return
      }
      await this.managedApi.restAssessmentController.updateScore(
        this.conversation.id,
        true
      )
      this.$emit("updated")
    },
  },
  components: { SvgAgree, SvgDecline },
})
</script>

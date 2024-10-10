<template>
  <div>
    <div v-if="!isDisabled && conversation.maker === undefined">
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
        @click="decline"
        width="30px"
        height="30px"
        data-testid="DeclineButton"
      />
    </div>
    <div v-else>
      <span>{{ message }}</span>
    </div>
    <div v-if="conversation.maker === true && isDisabled">
      <span>Accepted</span>
    </div>
    <div v-if="conversation.maker === false && isDisabled">
      <span>Rejected</span>
    </div>
  </div>
</template>

<script lang="ts">
import SvgAgree from "@/components/svgs/SvgAgree.vue"
import SvgDecline from "@/components/svgs/SvgDecline.vue"
import { defineComponent, ref } from "vue"
import usePopups from "../commons/Popups/usePopups"
import useLoadingApi from "@/managedApi/useLoadingApi"

const { popups } = usePopups()

export default defineComponent({
  setup() {
    const isDisabled = ref(false)
    const message = ref("")

    return { ...useLoadingApi(), isDisabled, message }
  },
  props: {
    conversation: {
      id: Number,
      type: Object,
      required: true,
    },
  },
  methods: {
    async agree() {
      if (this.conversation.id === undefined) {
        return
      }
      const response =
        await this.managedApi.restAssessmentController.updateScore(
          123,
          this.conversation.id,
          true
        )

      if (response !== undefined) {
        popups.alert("Feedback is Accepted")
        this.isDisabled = true
        this.message = "Accepted"
      }
    },
    async decline() {
      if (this.conversation.id === undefined) {
        return
      }
      const response =
        await this.managedApi.restAssessmentController.updateScore(
          123,
          this.conversation.id,
          false
        )

      if (response !== undefined) {
        popups.alert("Feedback is Rejected")
        this.isDisabled = true
        this.message = "Rejected"
      }
    },
  },
  components: { SvgAgree, SvgDecline },
})
</script>

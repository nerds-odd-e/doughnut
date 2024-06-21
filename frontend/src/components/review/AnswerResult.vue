<template>
  <div class="alert alert-success" v-if="answeredQuestion.correct">
    Correct!
  </div>
  <div class="alert alert-danger" v-else>
    <strong>
      {{ "Your answer `" + answeredQuestion.answerDisplay + "` is incorrect." }}
    </strong>
  </div>
</template>

<script lang="ts">
import { AnsweredQuestion } from "@/generated/backend"
import { PropType, defineComponent } from "vue"

export default defineComponent({
  props: {
    answeredQuestion: {
      type: Object as PropType<AnsweredQuestion>,
      required: true,
    },
  },
})
</script>

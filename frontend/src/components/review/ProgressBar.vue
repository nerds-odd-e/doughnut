<template>
  <StopRepeatButton />
  <PauseRepeatButton v-bind="{noteId, linkId, allowPause}"/>
  <span v-if="toRepeatCount !== null">{{finished}}/{{finished + toRepeatCount}} </span>
</template>
<script>
import StopRepeatButton from "./StopRepeatButton.vue"
import PauseRepeatButton from "./PauseRepeatButton.vue"

export default {
  components: { StopRepeatButton, PauseRepeatButton },
  props: {noteId: Number, linkId: Number, allowPause: { type: Boolean, default: true }, finished: Number, toRepeatCount: Number},
  methods: {
  },
}
</script>

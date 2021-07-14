<template>
  <StopRepeatButton />
  <PauseRepeatButton v-bind="{noteId, linkId}"/>
</template>
<script>
import StopRepeatButton from "./StopRepeatButton.vue"
import PauseRepeatButton from "./PauseRepeatButton.vue"

export default {
  components: { StopRepeatButton, PauseRepeatButton },
  props: {noteId: Number, linkId: Number},
  methods: {
  },
}
</script>

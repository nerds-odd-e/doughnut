<template>
  <CircleJoinForm v-bind="{invitationCode}"/>
</template>

<script>
import CircleJoinForm from "../components/circles/CircleJoinForm.vue"

export default {
  components: { CircleJoinForm},
  props: {invitationCode: Number},

}
</script>

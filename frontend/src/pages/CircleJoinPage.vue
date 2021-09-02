<template>
  <CircleJoinForm v-bind="{ invitationCode }" />
</template>

<script>
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import { loginOrRegister } from "../restful/restful";

export default {
  components: { CircleJoinForm },
  props: { invitationCode: Number, user: Object },

  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (!vm.user) {
        loginOrRegister();
        next(false);
      }
      next();
    });
  },
};
</script>

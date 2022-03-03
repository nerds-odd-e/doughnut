<template>
  <h1>Joining a Circle</h1>
  <form @submit.prevent.once="processForm">
    <TextInput
      scopeName="join-circle"
      field="invitationCode"
      v-model="formData.invitationCode"
      :autofocus="true"
      :errors="formErrors.invitationCode"
    />
    <input type="submit" value="Join" class="btn btn-primary" />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import { api } from "../../storedApi";

export default {
  components: { TextInput },
  props: { invitationCode: Number },

  data() {
    return {
      circle: null,
      loading: false,
      formData: { invitationCode: this.invitationCode },
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      this.loading = true;
      api().circleMethods.joinCircle(this.formData)
        .then((res) => {
          this.show = false;
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          });
        })
        .catch((res) => (this.formErrors = res))
        .finally(()=>this.loading = false);
    },
  },
};
</script>

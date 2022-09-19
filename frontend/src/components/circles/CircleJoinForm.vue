<template>
  <h1>Joining a Circle</h1>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="join-circle"
      field="invitationCode"
      v-model="formData.invitationCode"
      :autofocus="true"
      :errors="errors.invitationCode"
    />
    <input type="submit" value="Join" class="btn btn-primary" />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  components: { TextInput },
  props: { invitationCode: Number },

  data() {
    return {
      circle: null,
      formData: { invitationCode: this.invitationCode },
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.api.circleMethods
        .joinCircle(this.formData)
        .then((res) => {
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          });
        })
        .catch((err) => (this.errors = err));
    },
  },
};
</script>

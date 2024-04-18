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
import useLoadingApi from "@/managedApi/useLoadingApi";
import TextInput from "@/components/form/TextInput.vue";

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
      this.managedApi.restCircleController
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

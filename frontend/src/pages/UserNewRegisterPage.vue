<template>
  <ContainerPage
    v-bind="{
      loading,
      contentExists: true,
      title: 'Welcome, new user. Please create your profile',
    }"
  >
    <form @submit.prevent.once="processForm">
      <TextInput
        scope-name="user"
        field="name"
        v-model="formData.name"
        :autofocus="true"
        :errors="formErrors.name"
        :placeholder="Nickname"
      />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </form>
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import TextInput from "../components/form/TextInput.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi({ hasFormError: true });
  },
  emits: ["updateUser"],
  components: { ContainerPage, TextInput },
  data() {
    return {
      formData: {},
    };
  },
  methods: {
    processForm() {
      const user = this.api.userMethods.createUser(this.formData);
      this.$emit("updateUser", user);
    },
  },
};
</script>

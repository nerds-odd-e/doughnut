<template>
  <ContainerPage
    v-bind="{
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
        placeholder="Nickname"
      />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </form>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerPage from "./commons/ContainerPage.vue";
import TextInput from "../components/form/TextInput.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ hasFormError: true });
  },
  emits: ["updateUser"],
  components: { ContainerPage, TextInput },
  data() {
    return {
      formData: {
        name: undefined as undefined | string,
      } as Generated.User,
      formErrors: {
        name: undefined as undefined | string,
      },
    };
  },
  methods: {
    async processForm() {
      const user = await this.api.userMethods.createUser(this.formData);
      this.$emit("updateUser", user);
    },
  },
});
</script>

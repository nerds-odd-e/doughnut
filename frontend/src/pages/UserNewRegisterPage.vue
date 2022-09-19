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
        :errors="errors.name"
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
    return useLoadingApi();
  },
  emits: ["updateUser"],
  components: { ContainerPage, TextInput },
  data() {
    return {
      formData: {
        name: undefined as undefined | string,
      } as Generated.User,
      errors: {
        name: undefined as undefined | string,
      },
    };
  },
  methods: {
    async processForm() {
      try {
        const user = await this.api.userMethods.createUser(this.formData);
        this.$emit("updateUser", user);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (err: any) {
        this.errors = err;
      }
    },
  },
});
</script>

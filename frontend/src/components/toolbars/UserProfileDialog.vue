<template>
  <ContainerPage
    v-bind="{
      contentExists: formData !== undefined,
      title: 'Edit User Setting',
    }"
  >
    <div v-if="formData">
      <form @submit.prevent.once="processForm">
        <TextInput
          scope-name="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :errors="errors.name"
        />
        <TextInput
          scope-name="user"
          field="dailyNewNotesCount"
          v-model="formData.dailyNewNotesCount"
          :errors="errors.dailyNewNotesCount"
        />
        <TextInput
          scope-name="user"
          field="spaceIntervals"
          v-model="formData.spaceIntervals"
          :errors="errors.spaceIntervals"
        />
        <CheckInput
          scope-name="user"
          field="aiQuestionTypeOnlyForReview"
          v-model="formData.aiQuestionTypeOnlyForReview"
          :errors="errors.aiQuestionTypeOnlyForReview"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { User } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import ContainerPage from "@/pages/commons/ContainerPage.vue";
import CheckInput from "@/components/form/CheckInput.vue";
import TextInput from "@/components/form/TextInput.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  components: { ContainerPage, TextInput, CheckInput },
  emits: ["user-updated"],
  data() {
    return {
      formData: undefined as undefined | User,
      errors: {} as Record<string, string>,
    };
  },
  methods: {
    async fetchData() {
      this.formData = await this.managedApi.restUserController.getUserProfile();
    },
    async processForm() {
      if (!this.formData) return;
      const updated = await this.managedApi.restUserController
        .updateUser(this.formData.id, this.formData)
        .catch((err) => {
          this.errors = err;
        });
      this.$emit("user-updated", updated);
    },
  },

  mounted() {
    this.fetchData();
  },
});
</script>

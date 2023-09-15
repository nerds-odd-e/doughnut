<template>
  <h2>This login page is for test and development only</h2>
  <p>
    It will redirect to the following url after login
    <br />
    {{ redirectAfterLogin }}
  </p>
  <div v-if="errorMessage" class="alert alert-danger">
    {{ errorMessage }}
  </div>

  <div class="vh-100 d-flex justify-content-center align-items-center">
    <form @submit.prevent="handleSubmit" class="text-center form-signin">
      <div class="mb-3">
        <label for="username" class="form-label">Username</label>
        <input
          type="text"
          v-model="username"
          class="form-control"
          id="username"
          required
        />
      </div>
      <div class="mb-3">
        <label for="password" class="form-label">Password</label>
        <input
          type="password"
          v-model="password"
          class="form-control"
          id="password"
          required
        />
      </div>
      <button type="submit" class="btn btn-primary">Login</button>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      username: "",
      password: "",
      errorMessage: undefined,
    };
  },
  computed: {
    redirectAfterLogin(): string | undefined {
      if (!this.$route.query) return undefined;
      if (!this.$route.query.from) return undefined;

      return this.$route.query.from as string;
    },
  },
  methods: {
    handleSubmit() {
      // Encode username and password in Base64
      const token = btoa(`${this.username}:${this.password}`);

      fetch("/api/healthcheck", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Basic ${token}`,
        },
      })
        .then(() => {
          if (this.redirectAfterLogin) {
            window.location.href = this.redirectAfterLogin;
          } else {
            this.$router.push("/");
          }
        })
        .catch((err) => {
          this.errorMessage = err;
        });
    },
  },
});
</script>

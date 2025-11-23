<template>
  <h2>This login page is for test and development only</h2>
  <h3>Please sign in</h3>
  <p>
    It will redirect to the following url after login
    <br />
    {{ redirectAfterLogin }}
  </p>
  <div v-if="errorMessage" class="daisy-alert daisy-alert-danger">
    {{ errorMessage }}
  </div>

  <div class="daisy-min-h-screen daisy-flex daisy-justify-center daisy-items-center">
    <form @submit.prevent="handleSubmit" class="daisy-text-center">
      <div class="daisy-form-control daisy-mb-4">
        <label for="username" class="daisy-label">Username</label>
        <input
          type="text"
          v-model="username"
          class="daisy-input daisy-input-bordered"
          id="username"
          required
        />
      </div>
      <div class="daisy-form-control daisy-mb-4">
        <label for="password" class="daisy-label">Password</label>
        <input
          type="password"
          v-model="password"
          class="daisy-input daisy-input-bordered"
          id="password"
          required
        />
      </div>
      <button type="submit" id="login-button" class="daisy-btn daisy-btn-primary">Login</button>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue"

export default defineComponent({
  data() {
    return {
      username: "",
      password: "",
      errorMessage: undefined,
    }
  },
  computed: {
    redirectAfterLogin(): string | undefined {
      if (!this.$route.query) return undefined
      if (!this.$route.query.from) return undefined

      return this.$route.query.from as string
    },
  },
  methods: {
    handleSubmit() {
      // Encode username and password in Base64
      const token = btoa(`${this.username}:${this.password}`)

      fetch("/api/healthcheck", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Basic ${token}`,
        },
      })
        .then(() => {
          if (this.redirectAfterLogin) {
            window.location.href = this.redirectAfterLogin
          } else {
            window.location.href = "/"
          }
        })
        .catch((err) => {
          this.errorMessage = err
        })
    },
  },
})
</script>

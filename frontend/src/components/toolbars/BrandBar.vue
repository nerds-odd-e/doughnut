<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <router-link class="navbar-brand" :to="{ name: 'root' }">
      <img
        src="/odd-e.png"
        width="35"
        height="35"
        class="d-inline-block align-top"
        alt=""
      />
      Doughnut
    </router-link>
  </nav>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({});
</script>

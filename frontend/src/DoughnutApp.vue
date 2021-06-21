<script>
  export default {
    methods: {
      redirectTo(params) {
        this.$router.push(params)
      }
    }
  }
</script>

<template>
  <router-view @redirect="redirectTo($event)"></router-view>
</template>

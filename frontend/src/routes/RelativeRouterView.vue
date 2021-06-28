<template>
  <router-view v-bind="{staticInfo}"/>
</template>

<script>
export default {
  props: { staticInfo: Object }
}
</script>


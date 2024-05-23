<template>
  <mark ref="scrollRef" />
</template>

<script setup lang="ts">
import { Ref, nextTick, onMounted, ref } from "vue";

const scrollRef: Ref<HTMLElement | null> = ref(null);

onMounted(async () => {
  await nextTick();
  scrollRef.value?.scrollIntoView({ behavior: "smooth" });
});
</script>

<style scoped>
mark {
  display: block;
  width: 0;
  height: 0;
  visibility: hidden;
}
</style>

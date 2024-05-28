<template>
  <mark ref="scrollRef" />
</template>

<script setup lang="ts">
import "intersection-observer";
import { Ref, nextTick, onMounted, ref, onBeforeUnmount } from "vue";

const scrollRef: Ref<HTMLElement | null> = ref(null);

let observer: IntersectionObserver | null = null;

onMounted(async () => {
  await nextTick();

  const handleIntersection = (entries: IntersectionObserverEntry[]) => {
    if (entries[0] && entries[0].isIntersecting === false) {
      scrollRef.value?.scrollIntoView({ behavior: "smooth" });
    }
  };

  observer = new IntersectionObserver(handleIntersection, {
    root: null, // Use the viewport as the root
    threshold: 0, // Trigger when the element is not visible at all
  });

  if (scrollRef.value) {
    observer.observe(scrollRef.value);
  }
});

onBeforeUnmount(() => {
  if (scrollRef.value) {
    observer.unobserve(scrollRef.value);
  }
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

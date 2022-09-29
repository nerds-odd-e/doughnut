<template>
  <div>
    <div class="location-img" v-for="image of renderPhotos" :key="image.alt">
      <img :alt="image.alt" class="img-fluid" :src="image.url" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

interface LocationProps {
  photoUrl?: string | null;
  mapUrl?: string | null;
}

interface ImageInformation {
  alt: string;
  url: string;
}

const props = defineProps<LocationProps>();
const renderPhotos = computed(() => {
  const imageDetails: ImageInformation[] = [
    {
      alt: "location photo",
      url: props?.photoUrl || "",
    },
    {
      alt: "location map",
      url: props?.mapUrl || "",
    },
  ];

  return imageDetails.filter((image) => image.url.length > 0);
});
</script>
<style scoped>
.location-img {
  position: relative;
  display: inline-block;
}
</style>

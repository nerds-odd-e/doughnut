<template>
  <div
    style="position: relative; display: inline-block"
    id="country-photo-picture"
  >
    <img
      class="img-fluid"
      data-testid="country-photo"
      :src="photoUrl"
      v-if="photoUrl.length > 0"
    />
  </div>
</template>

<script setup lang="ts">
interface LocationProps {
  photoUrl: string;
  mapUrl: string;
}

defineProps<LocationProps>();
</script>

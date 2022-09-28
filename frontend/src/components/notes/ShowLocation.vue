<template>
  <div>
    <div
      style="position: relative; display: inline-block"
      v-if="photoUrl.length > 0"
    >
      <img
        alt="location photo"
        class="img-fluid"
        data-testid="location-photo"
        :src="photoUrl"
      />
    </div>
    <div
      style="position: relative; display: inline-block"
      v-if="mapUrl.length > 0"
    >
      <img
        alt="location map"
        class="img-fluid"
        data-testid="location-map"
        :src="mapUrl"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
interface LocationProps {
  photoUrl?: string;
  mapUrl?: string;
}

withDefaults(defineProps<LocationProps>(), {
  photoUrl: "",
  mapUrl: "",
});
</script>

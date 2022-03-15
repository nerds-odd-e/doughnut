<template>
  <BasicBreadcrumb v-bind="{ancestors}">
    <template #topLink v-if="notebook">
      <li v-if="fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!circle">
          <router-link :to="{ name: 'notebooks' }">Top</router-link>
        </li>
        <template v-else>
          <li class="breadcrumb-item">
            <router-link :to="{ name: 'circles' }">Circles</router-link>
          </li>
          <li class="breadcrumb-item">
            <router-link
              :to="{ name: 'circleShow', params: { circleId: circle.id } }"
              >{{ circle.name }}</router-link
            >
          </li>
        </template>
      </template>
    </template>
    <template v-slot:additional>
      <slot />
    </template>
  </BasicBreadcrumb>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

export default defineComponent({
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object as PropType<Generated.NotebookViewedByUser>,
  },
  components: {
    BasicBreadcrumb,
  },
  computed: {
    fromBazaar() {
      return this.notebook?.fromBazaar;
    },
    circle() {
      return this.notebook ? this.notebook.ownership.circle : null;
    },
  },
});
</script>

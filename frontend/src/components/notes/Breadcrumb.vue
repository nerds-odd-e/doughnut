<template>
  <BasicBreadcrumb :ancestors="ancestors">
    <template #topLink v-if="notebook">
      <li v-if="!owns" class="breadcrumb-item">
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

<script>
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

export default {
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object,
    owns: { type: Boolean, required: true },
  },
  components: {
    BasicBreadcrumb,
  },
  computed: {
    circle() {
      return !!this.notebook ? this.notebook.ownership.circle : null;
    },
  },
};
</script>

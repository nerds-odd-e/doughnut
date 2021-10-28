<template>
  <ol :style="`--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);`"
       class="breadcrumb bg-light bg-gradient">
    <template v-if="notebook">
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
    <slot />
  </ol>
</template>

<script>
import NoteTitleWithLink from "./NoteTitleWithLink.vue";

export default {
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object,
    owns: { type: Boolean, required: true },
  },
  components: {
    NoteTitleWithLink,
  },
  computed: {
    circle() {
      return !!this.notebook ? this.notebook.ownership.circle : null;
    },
  },
};
</script>

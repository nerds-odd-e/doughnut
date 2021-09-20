<template>
  <NoteBreadcrumb :ancestors="ancestors">
    <template #topLink>
      <li v-if="!owns" class="breadcrumb-item"><router-link :to="{name: 'bazaar'}">Bazaar</router-link></li>
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
  </NoteBreadcrumb>

</template>

<script>
import NoteBreadcrumb from "./NoteBreadcrumb.vue";

export default {
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object,
    noteTitle: String,
    owns: { type: Boolean, required: true },
  },
  components: {
    NoteBreadcrumb,
  },
  computed: {
    circle() { return !!this.notebook ? this.notebook.ownership.circle : null }

  }
};
</script>

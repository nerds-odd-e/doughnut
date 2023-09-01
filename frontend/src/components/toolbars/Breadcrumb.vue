<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <li v-if="fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!cCircle">
          <router-link :to="{ name: 'notebooks' }">My Notes</router-link>
        </li>
        <template v-else>
          <li class="breadcrumb-item">
            <router-link
              :to="{ name: 'circleShow', params: { circleId: cCircle.id } }"
              >{{ cCircle.name }}</router-link
            >
          </li>
        </template>
      </template>
    </template>
    <template #additional>
      <slot />
    </template>
  </BasicBreadcrumb>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

export default defineComponent({
  props: {
    ancestors: Array,
    circle: Object as PropType<Generated.Circle>,
    notebook: Object as PropType<Generated.NotebookViewedByUser>,
  },
  components: {
    BasicBreadcrumb,
  },
  computed: {
    fromBazaar() {
      return this.notebook?.fromBazaar;
    },
    cCircle() {
      if (this.circle) return this.circle;
      return this.notebook ? this.notebook.ownership.circle : undefined;
    },
  },
});
</script>

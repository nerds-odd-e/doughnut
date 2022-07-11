<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <li>
        <PopupButton title="choose a circle">
          <template #button_face>
            <SvgForward />
          </template>
          <template #dialog_body="{ doneHandler }">
            <CircleSelector @done="doneHandler($event)" />
          </template>
        </PopupButton>
      </li>
      <li v-if="fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!circle">
          <router-link :to="{ name: 'notebooks' }">My Notes</router-link>
        </li>
        <template v-else>
          <li class="breadcrumb-item">
            <router-link
              :to="{ name: 'circleShow', params: { circleId: circle.id } }"
              >{{ circle.name }}</router-link
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
import SvgForward from "../svgs/SvgForward.vue";
import CircleSelector from "../circles/CircleSelector.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: {
    ancestors: Array,
    circle: Object as PropType<Generated.Circle>,
    fromBazaar: Boolean,
  },
  components: {
    BasicBreadcrumb,
    SvgForward,
    CircleSelector,
    PopupButton,
  },
});
</script>

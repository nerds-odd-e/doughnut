<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <BrandBar />
  </nav>
  <ToolbarFrame class="fixed-bottom-within-sidebar">
    <UserActionsButton
      v-bind="{ user }"
      @update-user="$emit('updateUser', $event)"
    />
  </ToolbarFrame>
  <LoadingPage v-bind="{ contentExists: circles }">
    <ul class="list-group">
      <li v-if="user?.admin" class="list-group-item">
        <router-link :to="{ name: 'adminDashboard' }">
          Admin Dashboard
        </router-link>
      </li>
      <li class="list-group-item">
        <ReviewButton class="btn" />
      </li>
      <li class="list-group-item">
        <router-link :to="{ name: 'notebooks' }"> My Notebooks </router-link>
      </li>
      <li class="list-group-item">
        <router-link :to="{ name: 'bazaar' }"> Bazaar </router-link>
      </li>
      <template v-if="circles">
        <li class="list-group-item" v-for="circle in circles" :key="circle.id">
          <router-link
            :to="{ name: 'circleShow', params: { circleId: circle.id } }"
          >
            {{ circle.name }}
          </router-link>
        </li>
      </template>
    </ul>
    <div class="btn-group">
      <PopButton btn-class="btn btn-secondary" title="Create a new circle">
        <CircleNewDialog />
      </PopButton>
      <router-link btn-class="btn btn-primary" :to="{ name: 'circleJoin' }">
        Join a circle
      </router-link>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import PopButton from "../commons/Popups/PopButton.vue";
import CircleNewDialog from "../circles/CircleNewDialog.vue";
import BrandBar from "./BrandBar.vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import ReviewButton from "./ReviewButton.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    user: { type: Object as PropType<Generated.User> },
  },
  data() {
    return {
      circles: undefined as Generated.Circle[] | undefined,
    };
  },
  emits: ["updateUser"],
  methods: {
    fetchData() {
      this.api.circleMethods.getCirclesOfCurrentUser().then((res) => {
        this.circles = res;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
  components: {
    PopButton,
    CircleNewDialog,
    LoadingPage,
    BrandBar,
    ToolbarFrame,
    ReviewButton,
  },
});
</script>

<style lang="scss" scoped>
.fixed-bottom-within-sidebar {
  position: absolute;
  bottom: 0;
  width: 100%;
  z-index: 1000;
}
</style>

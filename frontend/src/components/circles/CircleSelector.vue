<template>
  <ContainerPage
    v-bind="{ loading, contentExists: !!circles, title: 'My Circles' }"
  >
    <PopupButton title="Create a new circle">
      <template #dialog_body="{ doneHandler }">
        <CircleNewDialog @done="$emit('done', doneHandler($event))" />
      </template>
    </PopupButton>
    <div v-if="!!circles">
      <div class="row">
        <div
          class="col-12 col-sm-6 col-md-4 col-lg-3"
          v-for="circle in circles"
          :key="circle.id"
        >
          <div class="card rounded-circle text-center">
            <div class="card-title">
              <router-link
                :to="{ name: 'circleShow', params: { circleId: circle.id } }"
              >
                {{ circle.name }}
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
    <CircleJoinForm />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerPage from "@/pages/commons/ContainerPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import PopupButton from "../commons/Popups/PopupButton.vue";
import CircleNewDialog from "./CircleNewDialog.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  emits: ["done"],
  data() {
    return {
      circles: null as Generated.Circle[] | null,
    };
  },
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
  components: { ContainerPage, PopupButton, CircleNewDialog },
});
</script>

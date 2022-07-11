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
      <ul class="list-group" v-for="circle in circles" :key="circle.id">
        <li class="list-group-item">
          <div class="card-title">
            <router-link
              :to="{ name: 'circleShow', params: { circleId: circle.id } }"
            >
              {{ circle.name }}
            </router-link>
          </div>
        </li>
      </ul>
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
import CircleJoinForm from "./CircleJoinForm.vue";

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
  components: { ContainerPage, PopupButton, CircleNewDialog, CircleJoinForm },
});
</script>

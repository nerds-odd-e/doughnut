<template>
  <ContainerPage
    v-bind="{ loading, contentExists: !!circles, title: 'My Circles' }"
  >
    <PopupButton title="Create a new circle">
      <template #dialog_body="{ doneHandler }">
        <CircleNewDialog @done="doneHandler($event)" />
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

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import PopupButton from "../components/commons/Popups/PopupButton.vue";
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import CircleNewDialog from "../components/circles/CircleNewDialog.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  components: { ContainerPage, PopupButton, CircleJoinForm, CircleNewDialog },
  data() {
    return {
      circles: null,
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
};
</script>

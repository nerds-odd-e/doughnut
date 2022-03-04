<template>
  <ContainerPage v-bind="{ loading, contentExists: !!circles, title: 'My Circles' }">
    <CircleNewButton @updated="fetchData()" />
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
import CircleNewButton from "../components/circles/CircleNewButton.vue";
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import api from  "../managedApi/api";

export default {
  components: { ContainerPage, CircleNewButton, CircleJoinForm },
  data() {
    return {
      loading: true,
      circles: null,
    };
  },
  methods: {
    fetchData() {
      api(this).circleMethods.getCirclesOfCurrentUser().then(
        (res) => {
          this.circles = res
        }
      )
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>

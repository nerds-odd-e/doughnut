<template>
<div class="container">
  <h2>My Circles</h2>
  <CircleNewButton @updated="fetchData()" />
  <LoadingPage v-bind="{ loading, contentExists: !!circles }">
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
  </LoadingPage>

  <CircleJoinForm />
</div>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import CircleNewButton from "../components/circles/CircleNewButton.vue";
import CircleJoinForm from "../components/circles/CircleJoinForm.vue";
import { restGet } from "../restful/restful";

export default {
  components: { LoadingPage, CircleNewButton, CircleJoinForm },
  data() {
    return {
      loading: true,
      circles: null,
    };
  },
  methods: {
    fetchData() {
      this.loading = true
      restGet(`/api/circles`).then(
        (res) => {
          this.circles = res
        }
      )
      .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>

<template>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
      <button
        class="navbar-toggler"
        type="button"
        data-toggle="collapse"
        data-target="#navbarSupportedContent"
        aria-controls="navbarSupportedContent"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <router-link class="navbar-brand" :to="{ name: 'root' }">
        <img
          src="/odd-e.png"
          width="30"
          height="30"
          class="d-inline-block align-top"
          alt=""
        />
        Doughnut
      </router-link>
      <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item active">
            <router-link class="nav-link" :to="{ name: 'notebooks' }"
              >My Notes</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'reviews' }"
              >Review</router-link>
          </li>

          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'circles' }"
              >Circles</router-link>
          </li>

          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'bazaar' }"
              >Bazaar</router-link>
          </li>

          <li v-if="environment=='testing'" class="nav-item">
            <router-link class="nav-link" :to="{ name: 'testability' }"
              >Testability</router-link>
          </li>

          <li v-if="featureToggle" class="nav-item">
            <em class="nav-link btn-danger">Feature Toggle is On </em>
          </li>

          <li class="nav-item" v-if="user && user.isDeveloper">
            <a class="nav-link" href="/failure-report-list">Failure Reports</a>
          </li>
        </ul>
        <div class="d-flex" v-if="user">
          <span style="margin-right: 10px" class="navbar-text">
            <router-link
              class="user-profile-link"
              :to="{ name: 'userProfile' }"
              >{{ user.name }}</router-link
            >
          </span>
          <div
            class="form-inline my-2 my-lg-0"
          >
            <button
              class="btn btn-outline-success me-2 my-sm-0"
              v-on:click="logout"
            >
              Logout
            </button>
          </div>
        </div>
        <button v-else class="btn btn-outline-primary me-2 my-sm-0" @click="login"
          >Login via Github
        </button>
      </div>
    </nav>
</template>

<script>
import loginOrRegisterAndHaltThisThread from '../../managedApi/restful/loginOrRegisterAndHaltThisThread';
import useStoredLoadingApi from '../../managedApi/useStoredLoadingApi';

export default ({
  setup() {
    return useStoredLoadingApi();
  },
  methods: {
    async logout() {
      await this.api.userMethods.logout()
      window.location.href = "/bazaar"
    },
    login() {
      loginOrRegisterAndHaltThisThread()
    }
  },
  computed: {
    user() { return this.piniaStore.currentUser },
    featureToggle() { return this.piniaStore.featureToggle },
    environment() { return this.piniaStore.environment },
  }
});
</script>

<template>
   <div th:fragment="header">
        <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
            <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <a class="navbar-brand" href="/#">
                <img src="/odd-e.png" width="30" height="30" class="d-inline-block align-top" alt="">
                Doughnut
            </a>
            <div class="collapse navbar-collapse" id="navbarSupportedContent">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item active ">
                        <router-link class="nav-link" :to="{name: 'notebooks'}">My Notes</router-link>
                    </li>
                    <li class="nav-item ">
                        <router-link class="nav-link" :to="{name: 'reviews'}">Review</router-link>
                    </li>
                    <li class="nav-item ">
                        <a class="nav-link" href="/circles">Circles</a>
                    </li>
                    <li class="nav-item ">
                        <a class="nav-link" href="/bazaar">Bazaar</a>
                    </li>
                    <li class="nav-item " v-if="user && user.isDeveloper">
                        <a class="nav-link" href="/failure-report-list">Failure Reports</a>
                    </li>
                </ul>
                <div class="d-flex" v-if="user">
                  <span style="margin-right:10px;" class="navbar-text">
                      <router-link class="user-profile-link" :to="{name: 'userProfile'}">{{user.name}}</router-link>
                  </span>
                  <form class="form-inline my-2 my-lg-0" th:action="@{/logout}" method="post">
                      <input class= "btn btn-outline-success me-2 my-sm-0" type= "submit" value="Logout">
                  </form>
                </div>
                <a v-else class= "btn btn-outline-primary me-2 my-sm-0" href="/login">Login via Github</a>
            </div>
        </nav>
    </div>
</template>

<script>
export default {
  props: {user: Object}
}
</script>
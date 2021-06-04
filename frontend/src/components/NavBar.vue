<template>
  <nav
    class="font-sans flex flex-col text-center content-center sm:flex-row sm:text-left sm:justify-between py-2 px-6 bg-black shadow sm:items-baseline w-full"
  >
    <div class="mb-2 sm:mb-0 inner">
      <a
        href="https://www.odd-e.com"
        class="text-3xl no-underline text-yellow-300 hover:text-white font-sans font-bold"
        >Odd-e Blog</a
      ><br />
      <span class="text-sm text-white">Home of the NERDs</span>
    </div>

    <div v-if="yearList.length" class="sm:mb-0 self-center">
      <a
        href="#"
        class="text-md no-underline text-yellow-300 hover:text-white ml-2 px-1"
        >Recent</a
      >
      <a
        v-for="year in yearList"
        :href="'/' + year"
        class="yearList text-md no-underline text-yellow-300 hover:text-white ml-2 px-1"
        >{{ year }}</a
      >
    </div>
  </nav>
</template>

<script>
export default {
  name: "NavBar",
  data() {
    return {
      yearList: [],
      apiYearsUrl: "/api/blog/year_list"
    };
  },

  mounted() {
    this.fetchBlogPostsYearList();
  },

  methods: {
    fetchBlogPostsYearList() {
      fetch(this.apiYearsUrl)
        .then(res => {
          console.table(res);
          return res.json();
        })
        .then(years => {
          console.table(years);
          this.yearList = years;
        })
        .catch(error => {
          alert(error);
        });
    }
  }
};
</script>

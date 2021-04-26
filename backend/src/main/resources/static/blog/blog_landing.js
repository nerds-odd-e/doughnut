
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: [],
    yearList: [],
    articleList: [],
    apiUrl: "../api/blog/posts_by_website_name/odd-e-blog",
    apiYearsUrl: "../api/blog/year_list"
  },
  created: function() {
    axios
        .get(this.apiUrl)
            .then((res) => {
                this.articleList = res.data;
            })
            .catch((res) => {
                alert("Error");
            });
    axios
        .get(this.apiYearsUrl)
            .then((res) => {
                this.yearList = res.data;
            })
            .catch((res) => {
                alert("Error");
            });
  }
});
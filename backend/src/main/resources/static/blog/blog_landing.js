
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: [],
    yearMonthList: [],
    articleList: [],
    apiUrl: "../api/blog_articles_by_website_name/odd-e-blog",
    apiYearsUrl: "../api/blog/yearmonth"
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
                this.yearMonthList = res.data;
            })
            .catch((res) => {
                alert("Error");
            });
  }
});
<template>
    <div class="font-sans antialiased">
        <nav class="flex items-center justify-between flex-wrap bg-black p-6">
            <div class="flex items-center flex-no-shrink text-white mr-6">
                <img class="fill-current h-8w-8 mr-2" alt="Odd-e Company Logo" src="../assets/odd-e.png" />
                <span class="font-semibold text-xl tracking-tight">NERDs Blog</span>
            </div>
            <div class="block sm:hidden">
                <button @click="toggle" class="flex items-center px-3 py-2 border rounded text-yellow-300 border-yellow-100 hover:text-white hover:border-white">
                    <svg class="fill-current h-3 w-3" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                        <title>Menu</title>
                        <path d="M0 3h20v2H0V3zm0 6h20v2H0V9zm0 6h20v2H0v-2z"/>
                    </svg>
                </button>
            </div>
            <div :class="open ? 'block' : 'hidden'" class="w-full flex-grow sm:flex sm:items-center sm:w-auto">
                <div class="text-sm sm:flex-grow">
                    <a href="#" class="no-underline block mt-4 sm:inline-block sm:mt-0 text-yellow-300 hover:text-white mr-4">Recent</a>
                </div>
            </div>
            <div v-for="item in yearList" class="yearList text-sm sm:flex-grow"> 
                <a href="#" class="no-underline block mt-4 sm:inline-block sm:mt-0 text-yellow-300 hover:text-white mr-4">{{ item }}</a>
            </div>
        </nav>
        <div class="col-lg-9" id="article-container" style=
        "width:100%;padding-left:50px; margin-bottom:50px;">
            <div v-if="articleList.length" v-for=
            "article in articleList" class="row article">
                <div class="row title" style=
                "width:100%;padding:10px;font-size:24px;font-weight:bold;">
                    {{ article.title }}
                </div>
                <div class="row authorName" style=
                "width:100%;padding:10px">
                    {{ article.author }}
                </div>
                <div class="row createdAt" style=
                "width:100%;padding:10px">
                    {{ article.createdDatetime }}
                </div>
                <div class="row content" style=
                "width:100%;padding:10px">
                    {{ article.description }}
                </div>
                <div style="width:100%; height: 30px;"></div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'Blog',
    data() {
        return {
            open: false,
            blogInfo: [],
            yearList: [],
            articleList: [],
            apiUrl: "/api/blog/posts_by_website_name/odd-e-blog",
            apiYearsUrl: "/api/blog/year_list"
        };
    },

    mounted() {
      this.fetchBlogPostsYearList();
      this.fetchBlogPosts();
    },

    methods: {
      toggle() {
        this.open = !this.open
      },
      
      fetchBlogPostsYearList() {
        fetch(this.apiYearsUrl)
        .then(res => {
            return res.json();
        })
        .then(years => {
            this.yearList = years;
        })
        .catch(error => {
            alert(error);
        });
      },
      
      fetchBlogPosts() {
        fetch(this.apiUrl)
        .then(res => {
            console.table(res);
            return res.json();
        })
        .then(articles => {
            console.table(articles);
            this.articleList = articles;
        })
        .catch(error => {
            alert(error);
        });
      }
    }
}
</script>

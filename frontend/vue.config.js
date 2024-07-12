module.exports = {
  chainWebpack: (config) => {
    config.resolve.alias.set("vue", "@vue/runtime-dom")
  },
}

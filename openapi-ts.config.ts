const { defineConfig } = require('@hey-api/openapi-ts')
const path = require('path')

module.exports = defineConfig({
  input: path.resolve('./open_api_docs.yaml'),

  output: {
    path: path.resolve('./frontend/src/generated/backend'),
    client: '@hey-api/client-fetch/fetch',
    name: 'DoughnutApi',
    clean: true,
  },

  typescript: {
    style: 'PascalCase',
    module: 'CommonJS',
    tsconfig: path.resolve('./tsconfig.openapi.json')
  }
})

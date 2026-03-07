import { defineConfig } from '@hey-api/openapi-ts'
import path from 'path'

export default defineConfig({
  input: path.resolve('./open_api_docs.yaml'),
  plugins: [
    '@hey-api/typescript',
    '@hey-api/client-fetch',
    {
      name: '@hey-api/sdk',
      operations: {
        strategy: 'byTags',
      },
    },
  ],
  output: {
    path: path.resolve('./packages/generated-backend'),
    name: 'DoughnutApi',
    clean: true,
  },
  typescript: {
    style: 'PascalCase',
    module: 'ESNext',
    tsconfig: path.resolve('./tsconfig.openapi.json'),
  },
})

import { defineConfig } from '@hey-api/openapi-ts'
import path from 'path'

export default defineConfig({
  input: path.resolve('./open_api_docs.yaml'),
  client: 'legacy/fetch',
  output: {
    path: path.resolve('./generated/backend'),
    name: 'DoughnutApi',
    clean: true,
  },
  typescript: {
    style: 'PascalCase',
    module: 'ESNext',
    tsconfig: path.resolve('./tsconfig.openapi.json'),
  },
})

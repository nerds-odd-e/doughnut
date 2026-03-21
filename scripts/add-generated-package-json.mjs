import { writeFileSync } from 'node:fs'
import { join } from 'node:path'

const outputPath =
  process.argv[2] ||
  join(process.cwd(), 'packages/generated/doughnut-backend-api')
const pkg = {
  name: '@generated/doughnut-backend-api',
  version: '0.1.0',
  private: true,
  type: 'module',
  exports: {
    '.': './index.ts',
    './client.gen': './client.gen.ts',
    './client/types.gen': './client/types.gen.ts',
    './sdk.gen': './sdk.gen.ts',
    './types.gen': './types.gen.ts',
    './*': './*',
  },
}
writeFileSync(
  join(outputPath, 'package.json'),
  `${JSON.stringify(pkg, null, 2)}\n`
)

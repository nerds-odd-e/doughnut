import assert from 'node:assert'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'
import { loadBackendPathHints } from './pathGoesToBackend.mjs'
import {
  extractRootAbsolutePathsFromHtml,
  collectRequiredStaticPathsFromFrontend,
} from './requiredStaticPathsFromFrontend.mjs'
import {
  gcpPathPatternMatches,
  gcpRoutesToStaticBucket,
} from './urlMapStaticRouting.mjs'
import {
  pathRulesFromUrlMapDoc,
  validateUrlMapAgainstHintsAndStaticPaths,
} from './validateUrlMapPathRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

const bucketSvc =
  'https://www.googleapis.com/compute/v1/projects/p/global/backendBuckets/b'

test('gcpPathPatternMatches', () => {
  assert(gcpPathPatternMatches('/assets/*', '/assets/foo'))
  assert(!gcpPathPatternMatches('/assets/*', '/asset/foo'))
  assert(gcpPathPatternMatches('/index.html', '/index.html'))
})

test('extractRootAbsolutePathsFromHtml strips query', () => {
  const paths = extractRootAbsolutePathsFromHtml(
    '<link rel="icon" href="/x.ico?v=1" /><script src="/a.js">'
  )
  assert.deepEqual(paths.sort(), ['/a.js', '/x.ico'])
})

test('collectRequiredStaticPathsFromFrontend includes icons from index.html', () => {
  const paths = collectRequiredStaticPathsFromFrontend(repoRoot)
  assert.ok(paths.includes('/odd-e.ico'))
  assert.ok(paths.includes('/odd-e.png'))
})

test('fixture URL map: root asset without pathRule fails', () => {
  const urlMap = {
    pathMatchers: [
      {
        name: 'doughnut-paths',
        pathRules: [
          { paths: ['/assets/*'], service: bucketSvc },
          { paths: ['/index.html'], service: bucketSvc },
          { paths: ['/'], service: bucketSvc },
        ],
      },
    ],
  }
  const hints = { exactPaths: ['/api/x'], pathPrefixes: ['/api/'] }
  const { failures } = validateUrlMapAgainstHintsAndStaticPaths({
    urlMapYamlText: YAML.stringify(urlMap),
    hints,
    requiredStaticPaths: ['/new-root-asset.woff2'],
  })
  assert.ok(
    failures.some((f) => f.includes('/new-root-asset.woff2')),
    failures.join('\n')
  )
})

test('fixture URL map: root asset with explicit rule passes', () => {
  const urlMap = {
    pathMatchers: [
      {
        name: 'doughnut-paths',
        pathRules: [
          { paths: ['/assets/*'], service: bucketSvc },
          { paths: ['/new-root-asset.woff2'], service: bucketSvc },
          { paths: ['/index.html'], service: bucketSvc },
          { paths: ['/'], service: bucketSvc },
        ],
      },
    ],
  }
  const hints = { exactPaths: [], pathPrefixes: [] }
  const { failures } = validateUrlMapAgainstHintsAndStaticPaths({
    urlMapYamlText: YAML.stringify(urlMap),
    hints,
    requiredStaticPaths: ['/new-root-asset.woff2'],
  })
  assert.equal(failures.length, 0, failures.join('\n'))
})

test('fixture: /assets/* sends probe path to bucket', () => {
  const urlMap = {
    pathMatchers: [
      {
        name: 'doughnut-paths',
        pathRules: [
          { paths: ['/assets/*'], service: bucketSvc },
          { paths: ['/index.html'], service: bucketSvc },
          { paths: ['/'], service: bucketSvc },
        ],
      },
    ],
  }
  const pr = pathRulesFromUrlMapDoc(urlMap)
  assert.ok(!('error' in pr))
  assert.ok(
    gcpRoutesToStaticBucket(
      '/assets/.doughnut-path-routing-probe',
      pr.pathRules
    )
  )
})

test('fixture: backend path must not match static bucket rule', () => {
  const urlMap = {
    pathMatchers: [
      {
        name: 'doughnut-paths',
        pathRules: [
          { paths: ['/api/*'], service: bucketSvc },
          { paths: ['/assets/*'], service: bucketSvc },
          { paths: ['/index.html'], service: bucketSvc },
          { paths: ['/'], service: bucketSvc },
        ],
      },
    ],
  }
  const hints = loadBackendPathHints(
    path.join(repoRoot, 'infra/gcp/path-routing/backend-path-hints.json')
  )
  const { failures } = validateUrlMapAgainstHintsAndStaticPaths({
    urlMapYamlText: YAML.stringify(urlMap),
    hints,
    requiredStaticPaths: [],
  })
  assert.ok(
    failures.some((f) => f.includes('/api/') || f.includes('backend-classified')),
    failures.join('\n')
  )
})

test('committed doughnut-app-service-map passes validation', () => {
  const yamlPath = path.join(
    repoRoot,
    'infra/gcp/url-maps/doughnut-app-service-map.yaml'
  )
  const raw = readFileSync(yamlPath, 'utf8')
  const hints = loadBackendPathHints(
    path.join(repoRoot, 'infra/gcp/path-routing/backend-path-hints.json')
  )
  const requiredStaticPaths = collectRequiredStaticPathsFromFrontend(repoRoot)
  const { failures } = validateUrlMapAgainstHintsAndStaticPaths({
    urlMapYamlText: raw,
    hints,
    requiredStaticPaths,
  })
  assert.equal(failures.length, 0, failures.join('\n'))
})

import assert from 'node:assert'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  generateApiSummary,
  renderApiSummary,
} from './generate-api-summary.mjs'

test('renderApiSummary groups endpoints by controller tag', () => {
  const summary = renderApiSummary({
    paths: {
      '/api/user': {
        get: {
          tags: ['user-controller'],
          operationId: 'getUserProfile',
          responses: {
            200: {
              content: {
                '*/*': {
                  schema: { $ref: '#/components/schemas/User' },
                },
              },
            },
          },
        },
        post: {
          tags: ['user-controller'],
          operationId: 'createUser',
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/User' },
              },
            },
          },
          responses: {
            200: {
              content: {
                '*/*': {
                  schema: { $ref: '#/components/schemas/User' },
                },
              },
            },
          },
        },
      },
    },
  })

  assert.match(summary, /## User Controller/)
  assert.match(
    summary,
    /`getUserProfile`: GET `\/api\/user` -> `GetUserProfileResponse` \(request: none; response body: User\)/
  )
  assert.match(
    summary,
    /`createUser`: POST `\/api\/user` -> `CreateUserResponse` \(request: `CreateUserData`; body: User; response body: User\)/
  )
})

test('renderApiSummary includes path and query request fields', () => {
  const summary = renderApiSummary({
    paths: {
      '/api/notebooks/{notebook}/notes': {
        get: {
          tags: ['notebook-controller'],
          operationId: 'getNotes',
          parameters: [
            { name: 'notebook', in: 'path' },
            { name: 'search', in: 'query' },
          ],
          responses: {
            200: {
              content: {
                '*/*': {
                  schema: {
                    type: 'array',
                    items: { $ref: '#/components/schemas/NoteDTO' },
                  },
                },
              },
            },
          },
        },
      },
    },
  })

  assert.match(
    summary,
    /request: `GetNotesData`; path: notebook; query: search/
  )
  assert.match(summary, /response body: Array<NoteDto>/)
})

test('renderApiSummary matches generated PascalCase for acronyms', () => {
  const summary = renderApiSummary({
    paths: {
      '/api/testability/clean_db_and_reset_testability_settings': {
        post: {
          tags: ['testability-rest-controller'],
          operationId: 'resetDBAndTestabilitySettings',
          responses: {
            200: {
              content: {
                '*/*': {
                  schema: { type: 'string' },
                },
              },
            },
          },
        },
      },
    },
  })

  assert.match(summary, /`ResetDbAndTestabilitySettingsResponse`/)
})

test('generateApiSummary writes a markdown summary from an OpenAPI file', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'api-summary-test-'))
  try {
    const inputPath = path.join(dir, 'open_api_docs.yaml')
    const outputPath = path.join(dir, 'api-summary.md')
    await writeFile(
      inputPath,
      [
        'openapi: 3.1.0',
        'paths:',
        '  /api/ping:',
        '    get:',
        '      tags: [health-controller]',
        '      operationId: ping',
        '      responses:',
        '        "204":',
        '          description: No Content',
      ].join('\n'),
      'utf8'
    )

    await generateApiSummary({ inputPath, outputPath })

    assert.match(
      await readFile(outputPath, 'utf8'),
      /`ping`: GET `\/api\/ping`/
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

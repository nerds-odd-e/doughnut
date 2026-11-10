import assert from 'node:assert/strict'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import { writeFragment } from './ci-proof-outcomes-fixtures.mjs'

const junitSuite = (classname, ...cases) =>
  `<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="${classname}" tests="${cases.length}">
${cases
  .map(({ name, time, body = '', classname: caseClass = classname }) => {
    const attrs = `name="${name}" classname="${caseClass}"${
      time === undefined ? '' : ` time="${time}"`
    }`
    return body === ''
      ? `  <testcase ${attrs}/>`
      : `  <testcase ${attrs}>${body}</testcase>`
  })
  .join('\n')}
</testsuite>
`

test('a JUnit report maps classname to the backend test source file', () => {
  const report = junitSuite('com.odde.donut.FooTest', {
    name: 'passes',
    time: '0.01',
  })

  assert.deepEqual(outcomeLines('junit', report), [
    'passed\tbackend/src/test/java/com/odde/donut/FooTest.java',
  ])
})

test('JUnit failure and error count as failed; skipped counts as other', () => {
  const report = junitSuite(
    'com.odde.donut.BarTest',
    { name: 'ok', time: '0.1' },
    {
      name: 'fails',
      time: '0.2',
      body: '<failure message="boom" type="AssertionError">stack</failure>',
    },
    {
      name: 'errors',
      time: '0.3',
      body: '<error message="crash" type="RuntimeException">stack</error>',
    },
    { name: 'ignored', time: '0', body: '<skipped/>' }
  )

  assert.deepEqual(outcomeLines('junit', report), [
    'passed\tbackend/src/test/java/com/odde/donut/BarTest.java',
    'failed\tbackend/src/test/java/com/odde/donut/BarTest.java',
    'failed\tbackend/src/test/java/com/odde/donut/BarTest.java',
    'other\tbackend/src/test/java/com/odde/donut/BarTest.java',
  ])
})

test('a nested JUnit classname maps to the outer class file', () => {
  const report = junitSuite('com.odde.donut.OuterTest', {
    name: 'nested',
    classname: 'com.odde.donut.OuterTest$Inner',
    time: '0.05',
  })

  assert.deepEqual(outcomeLines('junit', report), [
    'passed\tbackend/src/test/java/com/odde/donut/OuterTest.java',
  ])
})

test('a directory of JUnit XML yields one fragment line per testcase', (t) => {
  const timed = 'backend/src/test/java/com/odde/donut/TimedTest.java'
  const untimed = 'backend/src/test/java/com/odde/donut/UntimedTest.java'
  const { times, fragment } = writeFragment(
    t,
    'junit',
    'backend/src/test/java',
    {
      'TEST-TimedTest.xml': junitSuite(
        'com.odde.donut.TimedTest',
        {
          name: 'fails',
          time: '1.25',
          body: '<failure message="boom" type="AssertionError"/>',
        },
        { name: 'ok', time: '0.5' }
      ),
      'TEST-UntimedTest.xml': junitSuite('com.odde.donut.UntimedTest', {
        name: 'no-time',
      }),
      'other.txt': 'ignored',
    }
  )

  assert.equal(times, `1.25\t${timed}\n0.5\t${timed}\n`)
  assert.equal(fragment.unit, 'case')
  assert.equal(fragment.durationMs, 1750)
  assert.deepEqual(fragment.files, [
    { path: timed, passed: 1, failed: 1, other: 0, durationMs: 1750 },
    { path: untimed, passed: 1, failed: 0, other: 0, untimed: 1 },
  ])
})

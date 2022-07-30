const got = require('got');
const {
  buildState,
  BuildState,
  englishDictionary,
  timer,
} = require('../sound-monitor');

jest.mock('got');

const html = `
<div id="check_suite_2467107019">
        <div title="This workflow run completed successfully." class="d-flex flex-items-center flex-justify-center">
        <a class="Link--primary css-truncate css-truncate-target" aria-label="Run 976 of dough CI CD" href="/nerds-odd-e/doughnut/actions/runs/737396864">move code</a>
  `;

afterAll((done) => {
  clearInterval(timer);
  done();
});

test('get content from github action', async () => {
  got.get.mockResolvedValue({ body: html });
  const state = await buildState();
  expect(state).toMatchObject({
    status: 'completed successfully.',
    gitLog: 'move code',
    buildName: 'check_suite_2467107019',
  });
});

test('should not say anything is state not changed', () => {
  const state = new BuildState(
    'build1',
    'completed successfully.',
    'do something'
  );
  const state2 = new BuildState(
    'build1',
    'completed successfully.',
    'do something'
  );
  expect(state.diffToSentence(state2, englishDictionary)).toContain('');
});

test('found a new build', () => {
  const state = new BuildState(
    'build1',
    'completed successfully.',
    'do something'
  );
  const state2 = new BuildState(
    'build2',
    'completed successfully.',
    'do something'
  );
  expect(state.diffToSentence(state2, englishDictionary)).toContain(
    `A new build 'do something' completed successfully`
  );
});

test('found a new status', () => {
  const state = new BuildState(
    'build1',
    'is currently running.',
    'do something'
  );
  const state2 = new BuildState(
    'build1',
    'completed successfully.',
    'do something'
  );
  expect(state.diffToSentence(state2, englishDictionary)).toContain(
    `The build is currently running`
  );
});

const got = require('got');
const {
  buildState,
  BuildState,
  englishDictionary,
  timer,
} = require('../sound-monitor');

jest.mock('got');

const html = `
<div class="Box-row js-socket-channel js-updatable-content" id="check_suite_10845785161" data-channel="eyJjIjoiY2hlY2tfc3VpdGVzOjEwODQ1Nzg1MTYxIiwidCI6MTY3NTgzMjEwOH0=--d66330a43c420e89112639127d93d12d161a708d48d3d73fdf99e185b88d1f96" data-url="/nerds-odd-e/feature-teams-site/actions/workflow-run/10845785161">
  <div class="d-table col-12">
    <div class="d-table-cell v-align-top col-11 col-md-6 pl-4 position-relative">
      <div class="position-absolute left-0 checks-list-item-icon text-center">

<div class="d-flex flex-items-center flex-justify-center">
    <svg width="16" height="16" style="margin-top: 2px" class="octicon octicon-check-circle-fill color-fg-success" aria-label="completed successfully" viewBox="0 0 16 16" version="1.1" role="img"><path fill-rule="evenodd" d="M8 16A8 8 0 108 0a8 8 0 000 16zm3.78-9.72a.75.75 0 00-1.06-1.06L6.75 9.19 5.28 7.72a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.5-4.5z"></path></svg>
</div>

      </div>

      <span class="h4 d-inline-block text-bold lh-condensed mb-1 width-full">
        <a class="Link--primary css-truncate css-truncate-target" style="min-width: 100%" aria-label="Run 11 of Docker Image CI. trigger build" href="/nerds-odd-e/feature-teams-site/actions/runs/4120919627">trigger build</a>
      </span>

      <span class="d-block text-small color-fg-muted mb-1 mb-md-0">
        <span class="text-bold">Docker Image CI</span>
        #11:

        <span class="color-fg-muted">
            Commit <a class="Link--muted" href="/nerds-odd-e/feature-teams-site/commit/473895939fe6a3a1c64687061d2406b25b4d49d8">4738959</a>

            pushed
            by
              <a class="Link--muted" data-hovercard-type="user" data-hovercard-url="/users/terryyin/hovercard" data-octo-click="hovercard-link-click" data-octo-dimensions="link_type:self" href="/terryyin">terryyin</a>

        </span>
      </span>

      <div class="d-block d-md-none text-small">
        <span class="d-inline d-md-block lh-condensed color-fg-muted my-1 pr-2 pr-md-0" title="Start time">
  <svg aria-hidden="true" height="16" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="octicon octicon-calendar">
    <path fill-rule="evenodd" d="M4.75 0a.75.75 0 01.75.75V2h5V.75a.75.75 0 011.5 0V2h1.25c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0113.25 16H2.75A1.75 1.75 0 011 14.25V3.75C1 2.784 1.784 2 2.75 2H4V.75A.75.75 0 014.75 0zm0 3.5h8.5a.25.25 0 01.25.25V6h-11V3.75a.25.25 0 01.25-.25h2zm-2.25 4v6.75c0 .138.112.25.25.25h10.5a.25.25 0 00.25-.25V7.5h-11z"></path>
</svg>
  <relative-time tense="past" datetime="2023-02-08T05:53:30+01:00" data-view-component="true" title="Feb 8, 2023, 5:53 AM GMT+1">February 8, 2023 05:53</relative-time>
</span>

            <span class="color-fg-muted">
      <svg aria-hidden="true" height="16" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="octicon octicon-stopwatch">
    <path fill-rule="evenodd" d="M5.75.75A.75.75 0 016.5 0h3a.75.75 0 010 1.5h-.75v1l-.001.041a6.718 6.718 0 013.464 1.435l.007-.006.75-.75a.75.75 0 111.06 1.06l-.75.75-.006.007a6.75 6.75 0 11-10.548 0L2.72 5.03l-.75-.75a.75.75 0 011.06-1.06l.75.75.007.006A6.718 6.718 0 017.25 2.541a.756.756 0 010-.041v-1H6.5a.75.75 0 01-.75-.75zM8 14.5A5.25 5.25 0 108 4a5.25 5.25 0 000 10.5zm.389-6.7l1.33-1.33a.75.75 0 111.061 1.06L9.45 8.861A1.502 1.502 0 018 10.75a1.5 1.5 0 11.389-2.95z"></path>
</svg>
      <span>
        1m 37s
      </span>
    </span>

          <a target="_parent" class="d-inline-block branch-name css-truncate css-truncate-target my-0 my-md-1" style="max-width: 200px;" title="master" href="/nerds-odd-e/feature-teams-site">master</a>
      </div>
    </div>

    <div class="d-none d-md-table-cell v-align-middle col-4 pl-2 px-md-3 position-relative">
        <a target="_parent" class="d-inline-block branch-name css-truncate css-truncate-target my-0 my-md-1" style="max-width: 200px;" title="master" href="/nerds-odd-e/feature-teams-site">master</a>
    </div>

    <div class="d-table-cell v-align-middle col-1 col-md-3 text-small">
      <div class="d-flex flex-justify-between flex-items-center">
        <div class="d-none d-md-block">
          <span class="d-inline d-md-block lh-condensed color-fg-muted my-1 pr-2 pr-md-0" title="Start time">
  <svg aria-hidden="true" height="16" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="octicon octicon-calendar">
    <path fill-rule="evenodd" d="M4.75 0a.75.75 0 01.75.75V2h5V.75a.75.75 0 011.5 0V2h1.25c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0113.25 16H2.75A1.75 1.75 0 011 14.25V3.75C1 2.784 1.784 2 2.75 2H4V.75A.75.75 0 014.75 0zm0 3.5h8.5a.25.25 0 01.25.25V6h-11V3.75a.25.25 0 01.25-.25h2zm-2.25 4v6.75c0 .138.112.25.25.25h10.5a.25.25 0 00.25-.25V7.5h-11z"></path>
</svg>
  <relative-time tense="past" datetime="2023-02-08T05:53:30+01:00" data-view-component="true" title="Feb 8, 2023, 5:53 AM GMT+1">February 8, 2023 05:53</relative-time>
</span>

              <span class="color-fg-muted">
      <svg aria-hidden="true" height="16" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="octicon octicon-stopwatch">
    <path fill-rule="evenodd" d="M5.75.75A.75.75 0 016.5 0h3a.75.75 0 010 1.5h-.75v1l-.001.041a6.718 6.718 0 013.464 1.435l.007-.006.75-.75a.75.75 0 111.06 1.06l-.75.75-.006.007a6.75 6.75 0 11-10.548 0L2.72 5.03l-.75-.75a.75.75 0 011.06-1.06l.75.75.007.006A6.718 6.718 0 017.25 2.541a.756.756 0 010-.041v-1H6.5a.75.75 0 01-.75-.75zM8 14.5A5.25 5.25 0 108 4a5.25 5.25 0 000 10.5zm.389-6.7l1.33-1.33a.75.75 0 111.061 1.06L9.45 8.861A1.502 1.502 0 018 10.75a1.5 1.5 0 11.389-2.95z"></path>
</svg>
      <span>
        1m 37s
      </span>
    </span>

        </div>

        <div class="text-right">
            <details class="details-overlay details-reset position-relative d-inline-block ">
                <summary aria-haspopup="menu" data-view-component="true" class="timeline-comment-action btn-link">    <svg aria-label="Show options" aria-hidden="false" role="img" height="16" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="octicon octicon-kebab-horizontal">
    <path d="M8 9a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM1.5 9a1.5 1.5 0 100-3 1.5 1.5 0 000 3zm13 0a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"></path>
</svg>
</summary>
              <ul class="dropdown-menu dropdown-menu-sw show-more-popover color-fg-default anim-scale-in" style="width: 185px">


                  <li>
                    <a href="/nerds-odd-e/feature-teams-site/actions/runs/4120919627/workflow" class="dropdown-item btn-link">
                      View workflow file
                    </a>
                  </li>

                  <li>
                    <details data-view-component="true" class="details-overlay details-overlay-dark details-reset">
  <summary role="button" data-view-component="true" class="dropdown-item btn-link menu-item-danger">    Delete workflow run
</summary>
  <details-dialog src="/nerds-odd-e/feature-teams-site/actions/runs/4120919627/delete" aria-label="Delete workflow run" data-view-component="true" class="Box Box--overlay flex-column fast Box-overlay--wide overflow-y-hidden d-flex anim-fade-in text-left" role="dialog" aria-modal="true">    <include-fragment>
      <svg style="box-sizing: content-box; color: var(--color-icon-primary);" width="32" height="32" viewBox="0 0 16 16" fill="none" data-view-component="true" class="my-3 mx-auto d-block anim-rotate">
  <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-opacity="0.25" stroke-width="2" vector-effect="non-scaling-stroke"></circle>
  <path d="M15 8a7.002 7.002 0 00-7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" vector-effect="non-scaling-stroke"></path>
</svg>
    </include-fragment>
</details-dialog>
</details>
                  </li>
              </ul>
            </details>
        </div>
      </div>
    </div>
  </div>
</div>
  `;

afterAll((done) => {
  clearInterval(timer);
  done();
});

test('get content from github action', async () => {
  got.get.mockResolvedValue({ body: html });
  const state = await buildState();
  expect(state).toMatchObject({
    status: 'completed successfully',
    gitLog: 'trigger build',
    buildName: 'check_suite_10845785161',
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

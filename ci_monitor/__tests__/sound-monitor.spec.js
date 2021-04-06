const request = require('request');
const {BuildState} = require('../sound-monitor');

jest.mock('request');

const html = `
<div class="d-table col-12">
    <div class="d-table-cell v-align-top col-11 col-md-6 pl-4 position-relative">
      <div class="position-absolute left-0 checks-list-item-icon text-center">
        <div title="This workflow run completed successfully." class="d-flex flex-items-center flex-justify-center">
    <svg width="16" height="16" style="margin-top: 2px" class="octicon octicon-check-circle-fill color-green-5" viewBox="0 0 16 16" version="1.1" aria-hidden="true"><path fill-rule="evenodd" d="M8 16A8 8 0 108 0a8 8 0 000 16zm3.78-9.72a.75.75 0 00-1.06-1.06L6.75 9.19 5.28 7.72a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.5-4.5z"></path></svg>
</div>

      </div>

      <span class="h4 d-inline-block text-bold lh-condensed mb-1 width-full">
        <a class="Link--primary css-truncate css-truncate-target" style="min-width: 100%" aria-label="Run 897 of dough CI CD" href="/nerds-odd-e/doughnut/actions/runs/721384714">Upgrade to cypress@7.0.0</a>
      </span>

      <span class="d-block text-small color-text-tertiary mb-1 mb-md-0">
        <span class="text-bold">dough CI CD</span>
        #897:

        <span class="color-text-tertiary">
            Commit <a class="Link--muted" href="/nerds-odd-e/doughnut/commit/91b6fe1ec2e3691ae954cec1b68637c9b403f6fb">91b6fe1</a>

            pushed
            by
              <a class="Link--muted" data-hovercard-type="user" data-hovercard-url="/users/yeongsheng-tan/hovercard" data-octo-click="hovercard-link-click" data-octo-dimensions="link_type:self" href="/yeongsheng-tan">yeongsheng-tan</a>
              
        </span>
      </span>

      <div class="d-block d-md-none text-small">
        <span class="d-inline d-md-block lh-condensed color-text-secondary my-1 pr-2 pr-md-0" title="Start time">
  <svg class="octicon octicon-calendar" height="16" viewBox="0 0 16 16" version="1.1" width="16" aria-hidden="true"><path fill-rule="evenodd" d="M4.75 0a.75.75 0 01.75.75V2h5V.75a.75.75 0 011.5 0V2h1.25c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0113.25 16H2.75A1.75 1.75 0 011 14.25V3.75C1 2.784 1.784 2 2.75 2H4V.75A.75.75 0 014.75 0zm0 3.5h8.5a.25.25 0 01.25.25V6h-11V3.75a.25.25 0 01.25-.25h2zm-2.25 4v6.75c0 .138.112.25.25.25h10.5a.25.25 0 00.25-.25V7.5h-11z"></path></svg>
  <time-ago datetime="2021-04-06T05:08:00Z" class="no-wrap " title="6 Apr 2021, 13:08 GMT+8">1 hour ago</time-ago>
</span>

            <span class="color-text-secondary">
      <svg class="octicon octicon-stopwatch" height="16" viewBox="0 0 16 16" version="1.1" width="16" aria-hidden="true"><path fill-rule="evenodd" d="M5.75.75A.75.75 0 016.5 0h3a.75.75 0 010 1.5h-.75v1l-.001.041a6.718 6.718 0 013.464 1.435l.007-.006.75-.75a.75.75 0 111.06 1.06l-.75.75-.006.007a6.75 6.75 0 11-10.548 0L2.72 5.03l-.75-.75a.75.75 0 011.06-1.06l.75.75.007.006A6.718 6.718 0 017.25 2.541a.756.756 0 010-.041v-1H6.5a.75.75 0 01-.75-.75zM8 14.5A5.25 5.25 0 108 4a5.25 5.25 0 000 10.5zm.389-6.7l1.33-1.33a.75.75 0 111.061 1.06L9.45 8.861A1.502 1.502 0 018 10.75a1.5 1.5 0 11.389-2.95z"></path></svg>
      <span>
        8m 58s
      </span>
    </span>

          <a target="_parent" class="d-inline-block branch-name css-truncate css-truncate-target my-0 my-md-1" style="max-width: 200px;" title="main" href="/nerds-odd-e/doughnut">main</a>
      </div>
    </div>

    <div class="d-none d-md-table-cell v-align-middle col-4 pl-2 px-md-3 position-relative">
        <a target="_parent" class="d-inline-block branch-name css-truncate css-truncate-target my-0 my-md-1" style="max-width: 200px;" title="main" href="/nerds-odd-e/doughnut">main</a>
    </div>

    <div class="d-table-cell v-align-middle col-1 col-md-3 text-small">
      <div class="d-flex flex-justify-between flex-items-center">
        <div class="d-none d-md-block">
          <span class="d-inline d-md-block lh-condensed color-text-secondary my-1 pr-2 pr-md-0" title="Start time">
  <svg class="octicon octicon-calendar" height="16" viewBox="0 0 16 16" version="1.1" width="16" aria-hidden="true"><path fill-rule="evenodd" d="M4.75 0a.75.75 0 01.75.75V2h5V.75a.75.75 0 011.5 0V2h1.25c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0113.25 16H2.75A1.75 1.75 0 011 14.25V3.75C1 2.784 1.784 2 2.75 2H4V.75A.75.75 0 014.75 0zm0 3.5h8.5a.25.25 0 01.25.25V6h-11V3.75a.25.25 0 01.25-.25h2zm-2.25 4v6.75c0 .138.112.25.25.25h10.5a.25.25 0 00.25-.25V7.5h-11z"></path></svg>
  <time-ago datetime="2021-04-06T05:08:00Z" class="no-wrap " title="6 Apr 2021, 13:08 GMT+8">1 hour ago</time-ago>
</span>

              <span class="color-text-secondary">
      <svg class="octicon octicon-stopwatch" height="16" viewBox="0 0 16 16" version="1.1" width="16" aria-hidden="true"><path fill-rule="evenodd" d="M5.75.75A.75.75 0 016.5 0h3a.75.75 0 010 1.5h-.75v1l-.001.041a6.718 6.718 0 013.464 1.435l.007-.006.75-.75a.75.75 0 111.06 1.06l-.75.75-.006.007a6.75 6.75 0 11-10.548 0L2.72 5.03l-.75-.75a.75.75 0 011.06-1.06l.75.75.007.006A6.718 6.718 0 017.25 2.541a.756.756 0 010-.041v-1H6.5a.75.75 0 01-.75-.75zM8 14.5A5.25 5.25 0 108 4a5.25 5.25 0 000 10.5zm.389-6.7l1.33-1.33a.75.75 0 111.061 1.06L9.45 8.861A1.502 1.502 0 018 10.75a1.5 1.5 0 11.389-2.95z"></path></svg>
      <span>
        8m 58s
      </span>
    </span>

        </div>

        <div class="text-right">
            <details class="details-overlay details-reset position-relative d-inline-block ">
              <summary class="btn-link timeline-comment-action" aria-haspopup="menu">
                <svg aria-label="Show options" aria-hidden="false" class="octicon octicon-kebab-horizontal" height="16" viewBox="0 0 16 16" version="1.1" width="16"><path d="M8 9a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM1.5 9a1.5 1.5 0 100-3 1.5 1.5 0 000 3zm13 0a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"></path></svg>
              </summary>

              <ul class="dropdown-menu dropdown-menu-sw show-more-popover color-text-primary anim-scale-in" style="width: 185px">


                  <li>
                    <a href="/nerds-odd-e/doughnut/actions/runs/721384714/workflow" class="dropdown-item btn-link">
                      View workflow file
                    </a>
                  </li>

                  <li>
                    <details class="details-overlay details-overlay-dark details-reset ">
  <summary role="button" class="dropdown-item btn-link menu-item-danger ">    Delete workflow run
</summary>
  <details-dialog src="/nerds-odd-e/doughnut/actions/runs/721384714/delete" aria-label="Delete workflow run" class="Box Box--overlay flex-column anim-fade-in fast Box-overlay--wide d-flex text-left" role="dialog" aria-modal="true">    <include-fragment>
      <svg style="box-sizing: content-box; color: var(--color-icon-primary);" viewBox="0 0 16 16" fill="none" width="32" height="32" class="my-3 mx-auto d-block anim-rotate">
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
  `
test('get content from github action', () => {
  request.mockImplementation((url, cb)=>cb(null, null, html));
  const state = new BuildState("", "");
  return expect(state.nextState()).resolves.toMatchObject({toSay: 'A new push: Upgrade to cypress@7.0.0completed successfully.'});
});
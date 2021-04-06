const request = require('request');
const {soundMonitor} = require('../sound-monitor');

jest.mock('request');

test('get content from github action', () => {
  request.mockResolvedValue(11);
  soundMonitor(()=>{});
});
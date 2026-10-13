const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests/e2e', workers: 1, retries: 0, reporter: 'list',
  use: { browserName: 'chromium', headless: true, viewport: {width: 1100, height: 900},
    locale: 'en-GB', colorScheme: 'light', timezoneId: 'UTC', offline: true },
});

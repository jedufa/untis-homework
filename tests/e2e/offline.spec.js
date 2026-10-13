const { test, expect } = require('@playwright/test');
const { pathToFileURL } = require('node:url');
const path = require('node:path');
const fs = require('node:fs');
const url = pathToFileURL(path.resolve(__dirname, '../../docs/index.html')).href;

test.beforeEach(async ({page}) => {
  await page.addInitScript(() => {
    window.__networkAttempts = [];
    for (const name of ['fetch', 'XMLHttpRequest', 'WebSocket', 'EventSource']) {
      window[name] = function () { window.__networkAttempts.push(name); throw new Error('Network forbidden in offline demo'); };
    }
    navigator.sendBeacon = () => { window.__networkAttempts.push('beacon'); throw new Error('Network forbidden'); };
  });
});

test('homework completion, child separation and notes survive reload without network', async ({page}) => {
  const errors = [];
  const remoteRequests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('request', request => { if (/^https?:/.test(request.url())) remoteRequests.push(request.url()); });
  await page.goto(url);
  await expect(page.locator('#open-list .homework')).toHaveCount(3);
  await expect(page.locator('#status')).toHaveText('Offline demo');
  await page.locator('#open-list .homework[data-id="1"] [data-action]').click();
  await expect(page.locator('#open-list .homework')).toHaveCount(2);
  await expect(page.locator('#archive-list .homework[data-id="1"]')).toHaveClass(/completed/);
  await page.reload();
  await expect(page.locator('#open-list .homework')).toHaveCount(2);
  await page.locator('[data-child-tab="child2"]').click();
  await expect(page.locator('#open-list .homework')).toHaveCount(2);
  await page.locator('[data-child-view="notes"]').click();
  await page.locator('#add-note-button').click();
  await page.locator('#note-title-input').fill('Remember the music book');
  await page.locator('#note-text').fill('Fictional reminder saved only in this browser.');
  await page.locator('#note-form button[type="submit"]').click();
  await expect(page.locator('#note-list')).toContainText('Remember the music book');
  await page.reload();
  await expect(page.locator('#note-list')).toContainText('Remember the music book');
  await page.locator('[data-child-tab="child1"]').click();
  await expect(page.locator('#note-list')).not.toContainText('Remember the music book');
  expect(await page.evaluate(() => window.__networkAttempts)).toEqual([]);
  expect(remoteRequests).toEqual([]);
  expect(errors).toEqual([]);
});

test('real dashboard gallery and responsive layout', async ({page}) => {
  await page.goto(url);
  await expect(page.locator('#open-list .homework')).toHaveCount(3);
  const capture = async name => {
    if (!process.env.CAPTURE_DIR) return;
    fs.mkdirSync(process.env.CAPTURE_DIR, {recursive: true});
    await page.screenshot({path: path.join(process.env.CAPTURE_DIR, `${name}.png`), fullPage: true});
  };
  await capture('homework');
  await page.locator('#open-list .homework[data-id="1"] [data-action]').click();
  await expect(page.locator('#archive-list .homework[data-id="1"]')).toHaveClass(/completed/);
  await capture('completed');
  await page.locator('[data-child-view="notes"]').click();
  await expect(page.locator('#note-list')).toContainText('Ready for tomorrow');
  await capture('notes');
  await page.locator('[data-child-view="homework"]').click();
  await page.locator('[data-child-tab="child2"]').click();
  await expect(page.locator('#open-list .homework')).toHaveCount(2);
  await capture('family');
  for (const width of [390, 320]) {
    await page.setViewportSize({width, height: 900});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
  expect(await page.evaluate(() => window.__networkAttempts)).toEqual([]);
});

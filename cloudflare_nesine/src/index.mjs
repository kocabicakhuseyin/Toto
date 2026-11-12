/**
 * Private Toto -> Cloudflare Browser Run -> official Nesine Spor Toto page.
 *
 * This Worker never accepts Nesine credentials and NEVER clicks a button that
 * places a wager. Users sign in and decide whether to play via Live View.
 * The cloud browser is temporary: on Workers Free it consumes browser minutes.
 */

import { timingSafeEqual } from "node:crypto";
import { Buffer } from "node:buffer";
import { connect } from "@cloudflare/playwright";
import {
  chooseNesineLiveTarget,
  expectedChecked,
  isOfficialProgram,
  MATCH_COUNT,
  MAX_COLUMNS,
  NESINE_URL,
  normalizeColumns,
  validSessionId,
} from "./columns.mjs";

const SESSION_KEEP_ALIVE_MS = 300_000; // Five inactive minutes; free-tier quota is limited.
const NO_CACHE_HEADERS = {
  "Cache-Control": "no-store, private",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...NO_CACHE_HEADERS, "Content-Type": "application/json; charset=utf-8" },
  });
}

function authorized(request, env) {
  const secret = env.TOTO_NESINE_BRIDGE_TOKEN;
  if (typeof secret !== "string" || secret.length < 32) return false;
  const auth = request.headers.get("Authorization") || "";
  if (!auth.startsWith("Bearer ")) return false;
  const actual = Buffer.from(auth.slice(7), "utf8");
  const expected = Buffer.from(secret, "utf8");
  return actual.length === expected.length && timingSafeEqual(actual, expected);
}

async function payload(request) {
  if ((request.headers.get("Content-Length") | 0) > 4096) {
    throw new TypeError("İstek çok büyük.");
  }
  const body = await request.text();
  if (body.length > 4096) throw new TypeError("İstek çok büyük.");
  try {
    const parsed = JSON.parse(body);
    if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") throw new Error();
    return parsed;
  } catch {
    throw new TypeError("JSON gövdesi geçersiz.");
  }
}

function sessionIdFrom(value) {
  if (!validSessionId(value)) throw new TypeError("Oturum kimliği geçersiz.");
  return value;
}

async function withSession(env, sessionId, action) {
  const browser = await connect(env.BROWSER, sessionId);
  try {
    return await action(browser);
  } finally {
    // Browser obtained via connect(): .close() DISCONNECTS, not terminates.
    // The user's Live View and login remain alive until the session expires.
    await browser.close();
  }
}

function existingPage(browser) {
  const pages = browser.contexts().flatMap((context) => context.pages());
  return pages.find((page) => isOfficialProgram(page.url())) || pages.find(
    (page) => page.url() === "about:blank",
  ) || pages[0] || null;
}

async function programPage(browser) {
  let page = existingPage(browser);
  if (!page) {
    const context = browser.contexts()[0] || await browser.newContext({
      viewport: { width: 1920, height: 1080 },
    });
    page = await context.newPage();
  }
  if (!isOfficialProgram(page.url())) {
    await page.goto(NESINE_URL, { waitUntil: "domcontentloaded", timeout: 45_000 });
  }
  if (!isOfficialProgram(page.url())) {
    throw new Error("Nesine resmi Spor Toto sayfası açık değil.");
  }
  await page.locator("#m-c-14-3-2").waitFor({ state: "attached", timeout: 35_000 });
  return page;
}

async function liveUrl(env, sessionId) {
  const targets = await env.BROWSER.devtools.listTargets(sessionId);
  // A Playwright-owned tab may disappear after its CDP client disconnects.
  // Never hand the user the browser's initial about:blank tab: create a
  // persistent Browser Run target for the official program instead.
  let page = chooseNesineLiveTarget(targets);
  if (!page) {
    page = await env.BROWSER.devtools.newTarget(sessionId, NESINE_URL);
  }
  if (!page || typeof page.id !== "string" || !page.id) {
    throw new Error("Nesine için canlı sekme oluşturulamadı.");
  }
  const result = await env.BROWSER.getLiveView(sessionId, {
    targetId: page.id,
    mode: "tab",
    expiresInMs: 300_000,
  });
  const url = result?.devtoolsFrontendUrl;
  if (typeof url !== "string" || !url.startsWith("https://live.browser.run/")) {
    throw new Error("Canlı görünüm bağlantısı alınamadı.");
  }
  // This URL has a sensitive access token. Never log it or cache it.
  return url;
}

async function newSession(env) {
  const session = await env.BROWSER.acquire({ keepAlive: SESSION_KEEP_ALIVE_MS });
  try {
    await withSession(env, session.sessionId, async (browser) => {
      await programPage(browser);
    });
    return json({
      session_id: session.sessionId,
      live_view_url: await liveUrl(env, session.sessionId),
      page_url: NESINE_URL,
      browser_running: true,
      account_login_verified: false,
      wager_submitted: false,
    });
  } catch {
    await env.BROWSER.closeSession(session.sessionId).catch(() => {});
    return json({ detail: "Bulut tarayıcı Nesine sayfasını açamadı." }, 503);
  }
}

async function fill(env, body) {
  if (body.same_bulletin_confirmed !== true) {
    return json({ detail: "İki sitedeki 15 maçın haftası ve sırası önce karşılaştırılmalı." }, 400);
  }
  const sessionId = sessionIdFrom(body.session_id);
  const columns = normalizeColumns(body.columns);
  try {
    const result = await withSession(env, sessionId, async (browser) => {
      const page = await programPage(browser);
      const count = await page.locator('input[id^="m-c-"][type="checkbox"]').count();
      if (count !== MATCH_COUNT * MAX_COLUMNS * 3) {
        throw new Error("Nesine seçim alanı değişti.");
      }

      // Every A/B/C/D group is ONE exact column. A double choice would
      // silently widen the filtered matrix and is intentionally forbidden.
      for (let group = 0; group < MAX_COLUMNS; group++) {
        for (let match = 0; match < MATCH_COUNT; match++) {
          for (let outcome = 0; outcome < 3; outcome++) {
            const checkbox = page.locator(`#m-c-${match}-${group}-${outcome}`);
            const wanted = expectedChecked(match, group, outcome, columns);
            if (await checkbox.isChecked() !== wanted) {
              await checkbox.setChecked(wanted, { timeout: 10_000 });
            }
          }
        }
      }

      let checked = 0;
      for (let group = 0; group < MAX_COLUMNS; group++) {
        for (let match = 0; match < MATCH_COUNT; match++) {
          for (let outcome = 0; outcome < 3; outcome++) {
            const actual = await page.locator(`#m-c-${match}-${group}-${outcome}`).isChecked();
            if (actual !== expectedChecked(match, group, outcome, columns)) {
              throw new Error("Checkbox doğrulaması başarısız.");
            }
            if (actual) checked++;
          }
        }
      }
      return { checked_outcomes: checked, page_url: page.url() };
    });
    return json({
      filled_columns: columns.length,
      checked_outcomes: result.checked_outcomes,
      verified: true,
      page_url: result.page_url,
      session_id: sessionId,
      wager_submitted: false,
    });
  } catch {
    return json({
      detail: "İşaretleme doğrulanamadı. Tarayıcı oturumunun açık ve Nesine sayfasının erişilebilir olduğunu kontrol edin."
    }, 503);
  }
}

async function preview(env, sessionId) {
  try {
    const bytes = await withSession(env, sessionId, async (browser) => {
      const page = await programPage(browser);
      return page.screenshot({ type: "png", fullPage: false });
    });
    return new Response(bytes, {
      headers: { ...NO_CACHE_HEADERS, "Content-Type": "image/png" },
    });
  } catch {
    return json({ detail: "Önizleme alınamadı; oturum süresi dolmuş olabilir." }, 503);
  }
}

export default {
  async fetch(request, env) {
    if (!authorized(request, env)) return json({ detail: "Erişim reddedildi." }, 401);
    const { pathname, searchParams } = new URL(request.url);
    try {
      if (pathname === "/v1/nesine/session" && request.method === "POST") {
        return await newSession(env);
      }
      if (pathname === "/v1/nesine/status" && request.method === "GET") {
        const sessionId = searchParams.get("session_id");
        const active = validSessionId(sessionId) && !!(await env.BROWSER.getSession(sessionId));
        return json({ browser_running: active, account_login_verified: false, wager_submitted: false });
      }
      if (pathname === "/v1/nesine/live" && request.method === "GET") {
        const sessionId = sessionIdFrom(searchParams.get("session_id"));
        return json({ live_view_url: await liveUrl(env, sessionId), session_id: sessionId });
      }
      if (pathname === "/v1/nesine/fill" && request.method === "POST") {
        return await fill(env, await payload(request));
      }
      if (pathname === "/v1/nesine/preview.png" && request.method === "GET") {
        return await preview(env, sessionIdFrom(searchParams.get("session_id")));
      }
      if (pathname === "/v1/nesine/close" && request.method === "POST") {
        const sessionId = sessionIdFrom((await payload(request)).session_id);
        await env.BROWSER.closeSession(sessionId);
        return json({ browser_running: false, wager_submitted: false });
      }
    } catch (error) {
      if (error instanceof TypeError) return json({ detail: error.message }, 422);
      return json({ detail: "Bulut oturumuna ulaşılamadı. Süresi dolmuş olabilir." }, 503);
    }
    return json({ detail: "İşlem bulunamadı." }, 404);
  },
};

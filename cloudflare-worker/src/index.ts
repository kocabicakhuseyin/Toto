import puppeteer, { BrowserWorker } from "@cloudflare/puppeteer";

export interface Env {
  BROWSER: BrowserWorker;
  WORKER_AUTH_TOKEN?: string;
}

interface ColumnPayload {
  index: number;
  predictions: string[]; // 15 items: "1", "0", or "2"
}

interface NesineAutoPlayRequest {
  username: string;
  password: string;
  couponTitle: string;
  totalCostTL: number;
  dryRun?: boolean;
  columns: ColumnPayload[];
}

interface NesineAutoPlayResponse {
  success: boolean;
  status: "COMPLETED" | "REQUIRES_2FA" | "WAF_BLOCKED" | "LOGIN_FAILED" | "ERROR";
  sessionId: string;
  playedColumnsCount: number;
  steps: string[];
  screenshotBase64?: string;
  errorMessage?: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed. Use POST." }, 405);
    }

    // Optional Bearer token protection (configure via `wrangler secret put WORKER_AUTH_TOKEN`)
    if (env.WORKER_AUTH_TOKEN) {
      const authHeader = request.headers.get("Authorization") ?? "";
      if (authHeader !== `Bearer ${env.WORKER_AUTH_TOKEN}`) {
        return jsonResponse({ error: "Unauthorized" }, 401);
      }
    }

    let payload: NesineAutoPlayRequest;
    try {
      payload = await request.json<NesineAutoPlayRequest>();
    } catch {
      return jsonResponse({ error: "Invalid JSON payload" }, 400);
    }

    if (!payload.username || !payload.password || !payload.columns?.length) {
      return jsonResponse(
        { error: "username, password, and non-empty columns array are required." },
        400
      );
    }

    const sessionId = crypto.randomUUID();
    const steps: string[] = [];
    let browser;

    try {
      steps.push("Cloudflare Browser Rendering oturumu başlatılıyor...");
      browser = await puppeteer.launch(env.BROWSER);
      const page = await browser.newPage();

      await page.setViewport({ width: 1280, height: 900 });
      await page.setUserAgent(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
      );

      steps.push("nesine.com/sportoto sayfasına gidiliyor...");
      await page.goto("https://www.nesine.com/sportoto", {
        waitUntil: "networkidle2",
        timeout: 25000,
      });

      const pageTitle = await page.title();
      steps.push(`Sayfa yüklendi: ${pageTitle}`);

      // Check for WAF / Challenge screen
      const bodyText = await page.evaluate(() => document.body?.innerText ?? "");
      if (
        bodyText.includes("Checking your browser") ||
        bodyText.includes("Just a moment") ||
        bodyText.includes("Access Denied")
      ) {
        const shot = (await page.screenshot({ encoding: "base64" })) as string;
        return jsonResponse<NesineAutoPlayResponse>({
          success: false,
          status: "WAF_BLOCKED",
          sessionId,
          playedColumnsCount: 0,
          steps: [...steps, "⚠️ Nesine WAF/Bot koruması algılandı. Uygulama içi WebView otomasyonunu kullanın."],
          screenshotBase64: shot,
          errorMessage: "Nesine.com bulut tarayıcı oturumuna bot koruması (Challenge) uyguladı.",
        });
      }

      // Attempt login if login button / fields exist
      steps.push(`'${payload.username}' hesabı ile giriş kontrol ediliyor...`);
      const loginFilled = await page.evaluate(
        (user: string, pass: string) => {
          const loginBtn = document.querySelector<HTMLElement>(
            "[data-testid='login-button'], .login-btn, a[href*='uyelik'], button[class*='login']"
          );
          if (loginBtn) loginBtn.click();

          const userInput = document.querySelector<HTMLInputElement>(
            "input[name='username'], input[name='Username'], input[id*='user'], input[placeholder*='Üye']"
          );
          const passInput = document.querySelector<HTMLInputElement>(
            "input[name='password'], input[name='Password'], input[type='password']"
          );
          if (userInput && passInput) {
            userInput.focus();
            userInput.value = user;
            userInput.dispatchEvent(new Event("input", { bubbles: true }));
            passInput.focus();
            passInput.value = pass;
            passInput.dispatchEvent(new Event("input", { bubbles: true }));
            const submit = document.querySelector<HTMLElement>(
              "button[type='submit'], input[type='submit']"
            );
            submit?.click();
            return true;
          }
          return false;
        },
        payload.username,
        payload.password
      );

      if (loginFilled) {
        await new Promise((r) => setTimeout(r, 2500));
        const postLoginText = await page.evaluate(() => document.body?.innerText ?? "");
        if (
          postLoginText.includes("SMS Şifresi") ||
          postLoginText.includes("Doğrulama Kodu") ||
          postLoginText.includes("OTP")
        ) {
          const shot = (await page.screenshot({ encoding: "base64" })) as string;
          return jsonResponse<NesineAutoPlayResponse>({
            success: false,
            status: "REQUIRES_2FA",
            sessionId,
            playedColumnsCount: 0,
            steps: [...steps, "📲 Nesine SMS doğrulama (2FA) kodu istedi."],
            screenshotBase64: shot,
            errorMessage: "Nesine SMS doğrulaması gerekiyor. Uygulama İçi Tarayıcı Robotu ile devam edebilirsiniz.",
          });
        }
      }

      // Fill Spor Toto columns in batches of 4 (#m-c-{match}-{group}-{outcome} or fallback rows)
      let playedCount = 0;
      const chunkSize = 4;
      for (let start = 0; start < payload.columns.length; start += chunkSize) {
        const batchCols = payload.columns.slice(start, start + chunkSize);
        const batchFilled = await page.evaluate((cols: ColumnPayload[]) => {
          const hasNativeGrid = !!document.getElementById("m-c-0-0-0");
          if (hasNativeGrid) {
            // Clear all 4 slots first
            for (let g = 0; g < 4; g++) {
              for (let m = 0; m < 15; m++) {
                for (let o = 0; o < 3; o++) {
                  const box = document.getElementById(`m-c-${m}-${g}-${o}`) as HTMLInputElement | null;
                  if (box && box.checked) box.click();
                }
              }
            }
            let markedCols = 0;
            cols.forEach((col, g) => {
              let markedMatches = 0;
              for (let m = 0; m < 15 && m < col.predictions.length; m++) {
                const raw = (col.predictions[m] || "").trim().toUpperCase();
                const outcomeIdx = raw === "1" ? 0 : (raw === "X" || raw === "0") ? 1 : 2;
                const box = document.getElementById(`m-c-${m}-${g}-${outcomeIdx}`) as HTMLInputElement | null;
                if (box) {
                  if (!box.checked) box.click();
                  markedMatches++;
                }
              }
              if (markedMatches === 15) markedCols++;
            });
            return markedCols;
          }

          // Fallback row selector
          const rows = document.querySelectorAll(
            ".sportoto-row, [data-match-row], tr[data-match-no], .toto-match-item"
          );
          if (rows.length < 15) return 0;
          const firstCol = cols[0];
          if (!firstCol) return 0;
          for (let i = 0; i < 15; i++) {
            const pick = firstCol.predictions[i];
            const row = rows[i];
            const buttons = row.querySelectorAll<HTMLElement>("button, .outcome-btn, td[data-outcome], label");
            buttons.forEach((btn) => {
              const txt = btn.innerText.trim().toUpperCase();
              const normalized = pick === "0" ? "X" : pick === "X" ? "0" : pick;
              if (txt === pick || txt === normalized) {
                btn.click();
              }
            });
          }
          return 1;
        }, batchCols);

        playedCount += batchFilled;
        if (!payload.dryRun && batchFilled > 0) {
          await page.evaluate(() => {
            const playBtn = document.querySelector<HTMLElement>(
              "button[class*='play'], button[class*='oyna'], [data-testid='submit-coupon']"
            );
            playBtn?.click();
          });
          await new Promise((r) => setTimeout(r, 1200));
        }
      }

      steps.push(
        playedCount > 0
          ? `✅ ${playedCount} kolon Spor Toto kupon tablosuna işaretlendi.`
          : "ℹ️ Spor Toto maç satırları ekran görüntüsü için hazırlandı."
      );

      if (!payload.dryRun && playedCount > 0) {
        steps.push("🚀 Kupon onay isteği Nesine'ye iletildi.");
      } else {
        steps.push("🛡️ Önizleme Modu (Dry Run): Kupon sepete hazırlandı, son onay kullanıcıya bırakıldı.");
      }

      const screenshotBase64 = (await page.screenshot({ encoding: "base64" })) as string;

      return jsonResponse<NesineAutoPlayResponse>({
        success: true,
        status: "COMPLETED",
        sessionId,
        playedColumnsCount: playedCount,
        steps,
        screenshotBase64,
      });
    } catch (err) {
      return jsonResponse<NesineAutoPlayResponse>(
        {
          success: false,
          status: "ERROR",
          sessionId,
          playedColumnsCount: 0,
          steps,
          errorMessage: err instanceof Error ? err.message : String(err),
        },
        500
      );
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  },
};

function jsonResponse<T>(data: T, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

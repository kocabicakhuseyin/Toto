export const NESINE_URL = "https://www.nesine.com/sportoto";
export const MATCH_COUNT = 15;
export const MAX_COLUMNS = 4;
export const OUTCOMES = Object.freeze({ "1": 0, X: 1, "2": 2 });

export function normalizeColumns(raw) {
  if (!Array.isArray(raw) || raw.length < 1 || raw.length > MAX_COLUMNS) {
    throw new TypeError("Bir defada 1–4 kolon gönderilebilir.");
  }
  return raw.map((value, index) => {
    if (typeof value !== "string") {
      throw new TypeError(`${index + 1}. kolon metin olmalıdır.`);
    }
    const column = value.trim().toUpperCase().replaceAll("0", "X");
    if (column.length !== MATCH_COUNT || !/^[1X2]{15}$/.test(column)) {
      throw new TypeError(`${index + 1}. kolon 15 adet 1, 0/X veya 2 içermelidir.`);
    }
    return column;
  });
}

export function validSessionId(value) {
  return typeof value === "string" && /^[a-zA-Z0-9-]{16,80}$/.test(value);
}

export function isOfficialProgram(url) {
  try {
    const parsed = new URL(url);
    return parsed.protocol === "https:" && parsed.hostname === "www.nesine.com" &&
      parsed.pathname.replace(/\/$/, "") === "/sportoto";
  } catch {
    return false;
  }
}

export function isNesineSite(url) {
  try {
    const parsed = new URL(url);
    return parsed.protocol === "https:" && parsed.hostname === "www.nesine.com";
  } catch {
    return false;
  }
}

// Browser Run starts with an about:blank target. A Live View must be attached
// to the actual Nesine tab (or a login route on the same official site).
export function chooseNesineLiveTarget(targets) {
  if (!Array.isArray(targets)) return null;
  const pages = targets.filter((item) =>
    item?.type === "page" && typeof item.id === "string" && item.id.length > 0,
  );
  return pages.find((item) => isOfficialProgram(item.url)) ||
    pages.find((item) => isNesineSite(item.url)) || null;
}

export function expectedChecked(matchIndex, groupIndex, outcomeIndex, columns) {
  const column = columns[groupIndex];
  return column !== undefined && OUTCOMES[column[matchIndex]] === outcomeIndex;
}

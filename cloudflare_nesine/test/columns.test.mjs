import test from "node:test";
import assert from "node:assert/strict";
import {
  chooseNesineLiveTarget, expectedChecked, isOfficialProgram,
  isNesineSite, normalizeColumns, validSessionId,
} from "../src/columns.mjs";

test("draw alias 0 becomes X without widening a column", () => {
  assert.deepEqual(normalizeColumns(["111110111110111"]), ["11111X11111X111"]);
});

test("up to four exact 15-match columns", () => {
  assert.equal(normalizeColumns(["1".repeat(15), "X".repeat(15), "2".repeat(15), "0".repeat(15)]).length, 4);
  assert.throws(() => normalizeColumns([]));
  assert.throws(() => normalizeColumns(Array(5).fill("1".repeat(15))));
  assert.throws(() => normalizeColumns(["1".repeat(14)]));
  assert.throws(() => normalizeColumns(["1".repeat(14) + "A"]));
});

test("exact radio-like checkbox allocation for four groups", () => {
  const columns = normalizeColumns(["1".repeat(15), "X".repeat(15)]);
  let checkedCount = 0;
  for (let g = 0; g < 4; g++) for (let m = 0; m < 15; m++) {
    let perMatch = 0;
    for (let o = 0; o < 3; o++) {
      if (expectedChecked(m, g, o, columns)) { checkedCount++; perMatch++; }
    }
    assert.equal(perMatch, g < 2 ? 1 : 0);
  }
  assert.equal(checkedCount, 30);
});

test("reject redirects or untrusted hostnames", () => {
  assert.ok(isOfficialProgram("https://www.nesine.com/sportoto"));
  assert.ok(!isOfficialProgram("http://www.nesine.com/sportoto"));
  assert.ok(!isOfficialProgram("https://www.nesine.com.evil.test/sportoto"));
  assert.ok(!isOfficialProgram("https://www.nesine.com/iddaa"));
  assert.ok(isNesineSite("https://www.nesine.com/uye-girisi"));
  assert.ok(!isNesineSite("https://www.nesine.com.evil.test/uye-girisi"));
});

test("Live View ignores about:blank and prefers the official Sportoto tab", () => {
  const blank = { id: "blank", type: "page", url: "about:blank" };
  const login = { id: "login", type: "page", url: "https://www.nesine.com/uye-girisi" };
  const sportoto = { id: "program", type: "page", url: "https://www.nesine.com/sportoto" };
  assert.equal(chooseNesineLiveTarget([blank]), null);
  assert.equal(chooseNesineLiveTarget([blank, login])?.id, "login");
  assert.equal(chooseNesineLiveTarget([blank, login, sportoto])?.id, "program");
  assert.equal(chooseNesineLiveTarget([blank, { id: "fake", type: "page", url: "https://www.nesine.com.evil.test/sportoto" }]), null);
});

test("session identifiers have a constrained shape", () => {
  assert.ok(validSessionId("0f4f9938-398f-40f3-9cec-b108c552754d"));
  assert.ok(!validSessionId("../some-other-session"));
});

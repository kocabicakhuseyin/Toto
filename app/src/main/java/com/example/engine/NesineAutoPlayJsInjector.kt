package com.example.engine

import com.example.model.TotoColumn

/**
 * Generates DOM JavaScript scripts injected into Android WebView
 * when automating coupon entry on nesine.com/spor-toto.
 */
object NesineAutoPlayJsInjector {

    private fun escapeJsString(value: String): String {
        val sb = StringBuilder("\"")
        value.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    fun buildLoginFillScript(username: String, password: String): String {
        val safeUser = escapeJsString(username)
        val safePass = escapeJsString(password)
        return """
            (function() {
                try {
                    var loginTrigger = document.querySelector("[data-testid='login-button'], .login-btn, a[href*='uyelik'], button[class*='login']");
                    if (loginTrigger) { loginTrigger.click(); }
                    var userEl = document.querySelector("input[name='username'], input[name='Username'], input[id*='user'], input[placeholder*='Üye'], input[placeholder*='T.C.']");
                    var passEl = document.querySelector("input[name='password'], input[name='Password'], input[type='password']");
                    if (!userEl || !passEl) {
                        return JSON.stringify({ filled: false, reason: "Giriş formu bulunamadı (Zaten giriş yapılmış olabilir veya Giriş Yap ekranını açın)." });
                    }
                    var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
                    nativeSetter.call(userEl, $safeUser);
                    userEl.dispatchEvent(new Event("input", { bubbles: true }));
                    userEl.dispatchEvent(new Event("change", { bubbles: true }));
                    nativeSetter.call(passEl, $safePass);
                    passEl.dispatchEvent(new Event("input", { bubbles: true }));
                    passEl.dispatchEvent(new Event("change", { bubbles: true }));
                    return JSON.stringify({ filled: true, reason: "Kullanıcı adı ve şifre dolduruldu." });
                } catch (e) {
                    return JSON.stringify({ filled: false, reason: e.message });
                }
            })();
        """.trimIndent()
    }

    fun buildColumnFillScript(column: TotoColumn, slotIndex: Int = 0): String {
        val safeSlot = slotIndex.coerceIn(0, 3)
        val picksArray = column.predictions.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { "\"${it.code}\"" }

        return """
            (function() {
                try {
                    var picks = $picksArray;
                    var pageText = (document.body && document.body.innerText ? document.body.innerText : "").toLowerCase();
                    if (pageText.indexOf("bakiye yetersiz") !== -1 ||
                        pageText.indexOf("yetersiz bakiye") !== -1 ||
                        pageText.indexOf("bakiyeniz yetersiz") !== -1 ||
                        pageText.indexOf("yeterli bakiye bulunm") !== -1) {
                        return JSON.stringify({
                            success: false,
                            clickedCount: 0,
                            message: "Bakiye yetersiz"
                        });
                    }
                    var hasNativeGrid = !!document.getElementById("m-c-0-0-0");
                    if (hasNativeGrid) {
                        var marked = 0;
                        for (var m = 0; m < 15 && m < picks.length; m++) {
                            var p = (picks[m] || "").trim().toUpperCase();
                            var outcomeIdx = p === "1" ? 0 : (p === "X" || p === "0") ? 1 : 2;
                            for (var o = 0; o < 3; o++) {
                                var cell = document.getElementById("m-c-" + m + "-$safeSlot-" + o);
                                if (cell) {
                                    var want = (o === outcomeIdx);
                                    if (cell.checked !== want) { cell.click(); }
                                    if (want) { marked++; }
                                }
                            }
                        }
                        return JSON.stringify({
                            success: marked > 0,
                            clickedCount: marked,
                            message: "Kolon #${column.index} (Sütun ${safeSlot + 1}) için " + marked + "/15 maç işaretlendi."
                        });
                    }

                    var rowSelectors = [
                        ".sportoto-row",
                        "[data-match-row]",
                        "tr[data-match-no]",
                        ".toto-match-item",
                        "[class*='sportoto'] [class*='row']",
                        "table tbody tr"
                    ];
                    var rows = [];
                    for (var s = 0; s < rowSelectors.length; s++) {
                        var candidate = document.querySelectorAll(rowSelectors[s]);
                        if (candidate && candidate.length >= 15) {
                            rows = candidate;
                            break;
                        }
                    }
                    if (!rows || rows.length < 15) {
                        return JSON.stringify({
                            success: false,
                            clickedCount: 0,
                            message: "Yeni bülten yüklenmedi (Oynanacak 15 maçlık bülten bulunamadı, satır: " + (rows ? rows.length : 0) + ")."
                        });
                    }
                    var clicked = 0;
                    for (var i = 0; i < 15 && i < picks.length; i++) {
                        var pick = picks[i];
                        var altPick = pick === "0" ? "X" : (pick === "X" ? "0" : pick);
                        var row = rows[i];
                        var btns = row.querySelectorAll("button, a, label, td, span, div[role='button']");
                        for (var j = 0; j < btns.length; j++) {
                            var txt = (btns[j].innerText || "").trim().toUpperCase();
                            if (txt === pick || txt === altPick) {
                                btns[j].click();
                                clicked++;
                                break;
                            }
                        }
                    }
                    return JSON.stringify({
                        success: clicked > 0,
                        clickedCount: clicked,
                        message: "Kolon #${column.index} için " + clicked + "/15 maç işaretlendi."
                    });
                } catch (e) {
                    return JSON.stringify({ success: false, clickedCount: 0, message: e.message });
                }
            })();
        """.trimIndent()
    }
}

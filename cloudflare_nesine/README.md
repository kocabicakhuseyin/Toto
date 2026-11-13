# Toto → Nesine Spor Toto — Cloudflare Browser Run (deneme entegrasyonu)

Bu klasör, kişisel Toto Android uygulamasının **kendi Cloudflare hesabınızda** çalışan
geçici Chrome tarayıcısına erişmesi için bir Worker içerir. Amaç, ürettiğiniz
15 maçlık **kesin kolonları** Nesine'nin resmi Spor Toto formuna aktarmak ve
işaretlemeleri doğrulamaktır. **Para yatırma, kupon oynama, ödeme veya `Hemen
Oyna` düğmesine basma kodu yoktur.** Son kontrol ve işlem kullanıcıya aittir.

> Durum: JavaScript birim testleri ve söz dizimi kontrolü yapılmıştır. Henüz
> kullanıcının Cloudflare hesabına dağıtılmadı; gerçek Nesine formunda
> işaretleme ve Android APK entegrasyonu uçtan uca sınanmadı. Playground'daki
> ekranın açılması, checkbox kimliklerinin ve etkileşimin de çalıştığını
> tek başına kanıtlamaz.

## Neden VPS gerekmiyor?

Cloudflare **Browser Run**, Worker tarafından yönetilen uzak Chromium sunar.
Mevcut hesabınızın `Workers & Pages` ve `Browser Run` menüleri yeterlidir.
Workers Free planında Kasım 2026 itibarıyla **günde toplam 10 dakika** tarayıcı
kullanımı bulunur. Bu 350 kolonun 88 parti halinde düzenli oynanması için
uygun bir ücretsiz kapasite değildir; öncelikle **1–4 kolon denemesi** içindir.

Kaynak: https://developers.cloudflare.com/browser-run/limits/

## Kişisel Cloudflare hesabına kurulum

1. Bilgisayarda Node.js 20.19+ veya 22.12+ (Wrangler'ın güncel minimumunu
   `npx wrangler --version` ile denetleyin) ve npm kurulu olsun.
2. Bu klasörü açın ve aşağıdaki komutları çalıştırın:

   ```sh
   npm install
   npm test
   npx wrangler login
   npm run deploy
   ```

3. `npm run deploy` sonunda verilen `https://...workers.dev` adresini not edin.
4. **En az 32 karakterlik**, rastgele bir Worker erişim anahtarı oluşturun.
   Örneğin Windows PowerShell'de:

   ```powershell
   ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
   ```

5. Gizli anahtarı Cloudflare hesabına girin. Terminale anahtarı yapıştırırken
   çevrenizden gizleyin. Bize, sohbet ekranına veya GitHub'a göndermeyin:

   ```sh
   npx wrangler secret put TOTO_NESINE_BRIDGE_TOKEN
   ```

6. Android uygulamasındaki **Nesine • Bulut Chrome** bölümüne Worker HTTPS
   adresini ve bu gizli erişim anahtarını yazın. `Bulut Chrome'yu Aç` düğmesi
   uzak oturumu başlatır ve Cloudflare'ın **Live View** ekranını açar.
7. Hesaba giriş gerekiyorsa **yalnızca resmî Nesine sayfasında** kendiniz yapın.
   Nesine kullanıcı adını veya şifrenizi Toto uygulamasına girmeyin.
8. Toto'daki 15 maçın sırasını Nesine resmi bültenindeki sırayla karşılaştırıp
   ilgili kutuyu işaretleyin. `Chrome'da Doldur` düğmesi **1–4 kesin kolon**
   aktarır; ardından kontrol ekranını açın. Eşleşmeler doğrulanmadan Nesine'de
   onay vermeyin.
9. İşiniz bitince **`Bulut oturumunu kapat`** düğmesine basın. Aksi halde
   geçici oturum boşta beklerken de günlük dakikaları tüketebilir.

## Android tarafı

Güncellenmiş `NesineChromeBridgeDialog.kt` dosyası dağıtım ZIP'inde ayrıca
bulunur. Mevcut Toto Android kaynaklarında aynı dosyanın yerine entegre
edilip, mevcut kupon ekranındaki çağrısı korunmalıdır. Bu ZIP **APK değildir**;
Gradle projesinin tamamı olmadan Android derlemesi yapılamaz.

## HTTP arayüzü (özel, gizli anahtar zorunlu)

Tüm isteklerde `Authorization: Bearer <gizli anahtar>` kullanılmalıdır:

| İstek | İşlem |
| --- | --- |
| `POST /v1/nesine/session` | Yeni uzak tarayıcı + canlı ekran bağlantısı |
| `GET /v1/nesine/live?session_id=...` | Canlı ekran için yeni kısa ömürlü bağlantı |
| `GET /v1/nesine/status?session_id=...` | Oturum açık mı? |
| `POST /v1/nesine/fill` | Kesin kolonları işaretle ve 180 kutuyu doğrula |
| `GET /v1/nesine/preview.png?session_id=...` | Güncel ekran görüntüsü |
| `POST /v1/nesine/close` | Oturumu kapat, süre tüketimini durdur |

Aktarım örneği (içerik temsili):

```json
{
  "session_id": "<Cloudflare oturum kimliği>",
  "same_bulletin_confirmed": true,
  "columns": ["111111111111111", "111111111111112"]
}
```

- Bir kolon tam 15 adet `1`, `X` veya `2` içerir; `0` yazılmışsa `X` olarak
  yorumlanır. **Birden çok sonuç tek bir maça eklenmez**; matrisi genişletecek
  çift seçimler oluşturulmaz.
- Nesine sayfasının alan kimlikleri değişirse aktarım **durur**. Bu durumda
  kutu işaretlemek için alternatif bir yöntem veya görsel koordinat taklidi
  otomatik olarak denenmez.
- `wager_submitted:false` yanıtı **sadece bu yazılımın oynama tuşuna
  basmadığı** anlamına gelir; Nesine'de gerçekleşmiş işlem veya giriş durumu
  doğrulaması değildir.
- Live View URL'si erişim bilgisi taşır: ekran görüntüsü veya tam URL'sini
  paylaşmayın. Cloudflare tarafından çalıştırılan tarayıcıda yapılan girişin
  güvenlik ve gizlilik koşulları ayrıca değerlendirilmelidir.
- Nesine'nin hizmet koşullarına ve izinlerine uyulmalı; teknik erişim,
  otomasyona veya bahis yerleştirmeye izin verildiğini tek başına göstermez.

## Yerel test

```sh
npm test
node --check src/index.mjs
```

Teknik kaynaklar:

- https://developers.cloudflare.com/browser-run/playwright/
- https://developers.cloudflare.com/browser-run/features/live-view/
- https://developers.cloudflare.com/browser-run/reference/browser-binding-api/

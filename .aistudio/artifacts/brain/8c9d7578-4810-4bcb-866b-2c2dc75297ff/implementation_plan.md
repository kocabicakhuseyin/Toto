# Spor Toto Hedef 15 - Uygulama ve Mimari Planı

Kullanıcı tarafından paylaşılan "Spor Toto Hedef 15" ekran görüntüleri ve tercihleri doğrultusunda, Spor Toto kupon hazırlama, gelişmiş formül sistemleri, maç analizleri, canlı forum ve Nesine entegrasyonu sunan eksiksiz bir Android uygulaması geliştirilecektir.

---

## 1. Mimari ve Bileşen Yapısı

Uygulama modern **MVVM + Jetpack Compose** mimarisi ile inşa edilecektir:

- **UI Katmanı (Jetpack Compose & Material 3 - Bahisçi / Casino Sportsbook Teması):**
  - **Koyu Lüks Bahis Teması:** Gece obsidiyen zemin (`#090D16`, `#111827`, `#141E31`), Vegas Altını (`#F59E0B`, `#FBBF24`), zümrüt yeşili (`#10B981`) ve elektrik mavisi aksanlar, yüksek kontrastlı gümüş ve beyaz tipografi.
  - **"Oyna" ve Akıllı Site Seçimi:** "Nesinede otomatik oyna" ifadeleri kaldırılarak sade ve güçlü **"OYNA ⚡"** akışına geçilmiştir. Kullanıcı kolon başı 0.20 ₺ bedeli onaylayıp oynadıktan sonra yasal bahis sitesini (İddaa, Nesine, Bilyoner, Misli, Birebin, Oley) açılan özel seçim modalından belirler.
  - Alt gezinme (Bottom Navigation) veya üst bar ile hızlı geçiş:
    1. **Anasayfa & Formüller:** Casino jackpot kartı, garantili formül sihirbazı (12, 13, 14, 15), 9 kolonlu özel sistem, TXT/Excel yükleme alanı, anlık bakiye göstergesi.
    2. **Maç Analiz & Bülten (15 Maç):** 1-X-2 oran ve canlı bahis oranları, "🔄 Bülteni Çek" butonu ile Retrofit üzerinden canlı maç güncelleme.
    3. **Forum & Yazar Köşesi:** Kullanıcıların takımlarına göre armalı avatarları, kupon paylaşımı, canlı yorumlaşma.
    4. **Kuponlarım & OYNA Akışı (0.20 ₺ / Kolon):**
       - Kolon sayısı ve kolon başı 0.20 TL işlem ücreti hesaplaması.
       - Cüzdan bakiyesi kontrolü, tek dokunuşla **"OYNA ⚡"** butonu.
       - Bakiye yetersizse anında Google Play Bakiye Yükleme dialogu açılır.
       - Bakiye yeterliyse ücret tahsil edilerek 6 yasal bahis sitesinden birinin seçileceği yönlendirme ekranı açılır.
    5. **Profil & Google Play Cüzdanı:** Bakiye kartı, Google Play yükleme geçmişi, kulüp arması ve kullanıcı bilgileri.

- **Ödeme ve Google Play Faturalandırma Altyapısı (`com.example.billing`):**
  - `GooglePlayBillingManager`: 25 ₺ (125 Kolon), 50 ₺ (250 Kolon), 100 ₺ (500 Kolon), 250 ₺ (1250 Kolon) bakiye paketleri.
  - Güvenli sipariş numaraları (`GPA.xxxx-xxxx-xxxx-xxxxx`) ve anlık cüzdan bakiyesine bakiye aktarımı.
  - Her kolon için kesin `COLUMN_FEE_TL = 0.20` ₺ (20 kuruş) hesaplama ve tahsilat mekanizması.

- **Veri, Ağ ve İş Mantığı Katmanı:**
  - **Haftalık Spor Toto Bülten Modeli & Retrofit Servisi (`com.example.network`):**
    - `WeeklyBulletinResponse`: Hafta numarası (hafta 38..), sezon, başlangıç-bitiş tarihleri, tahmini büyük ikramiye ve 15 maçlık liste.
    - `BulletinMatchDto`: Maç numarası (1-15), ev sahibi ve deplasman takımları, maç başlama tarihi, lig/stadyum bilgisi, durum (`UPCOMING`, `LIVE`, `FINISHED`).
    - `LiveBettingOddsDto`: Canlı bahis oranları (1, X, 2 ve çifte şans oranları: 1X, 12, X2) ve son güncelleme zamanı.
    - `SporTotoApiService`: `@GET` istekleri ile `getCurrentWeeklyBulletin()`, `getBulletinByWeek()`, `getLiveOdds()` uç noktaları.
    - `NetworkModule`: OkHttpClient (zaman aşımları, loglama ve offline dayanıklılık interceptor'ı) ve Moshi JSON dönüştürücüsü.
  - **Bülten Repository Katmanı (`com.example.repository`):**
    - `SporTotoBulletinRepository` & `SporTotoBulletinRepositoryImpl`: Ağ üzerinden güncel bülteni çeker, önbelleğe alır ve domain `TotoMatch` modellerine dönüştürür.
    - `TotoViewModel` üzerinden tek dokunuşla ("🔄 Bülteni Çek") veya uygulama açılışında otomatik güncelleme desteği.
  - **Spor Toto Formül Motoru:**
    - 15 Garanti (N-0), 14 Garanti (N-1), 13 Garanti (N-2), 12 Garanti (N-3) matematiksel indirgeme algoritmaları.
    - 9 Kolonlu garantili özel sistem.
    - Sürpriz / Favori / Beraberlik sayı filtreleri.
    - Extra1x2 standart formatında TXT kolon okuma ve dışa aktarma.
  - **6 Platformlu Otomasyon Motoru (`PlatformAutomationManager`):**
    - Kolonları Nesine, İddaa, Bilyoner, Misli, Birebin ve Oley platformlarının formatlarına dönüştürme.
    - Uygulama Intent ve Web derin bağlantıları ile kullanıcı adı & şifre üzerinden otomatik kupon aktarımı.
  - **Forum & Sosyal Katman:**
    - Maçlara özel yorum dizileri, kupon ekleme, takım armaları ve üyelik yönetimi.

---

## 2. Kullanıcı Akışları ve Ekranlar

### A. Anasayfa ve Formül Merkezi (Ekran 2 & 4 Referansı)
- Kullanıcı karşılama alanı ("HUSEYIN_K - Otomatik Oynama Ücretsiz").
- Kırmızı Vurgulu Formüller:
  - *En İyi Spor Toto Formülü (Garantili Özel Sistem)*
  - *Garantili Formüller 9 Kolonlu*
- Yeşil Vurgulu Dışa/İçe Aktarma:
  - *Extra1X2 TXT Kolonlarını Yükle & Otomatik Oyna*
  - *Excel Kuponlarını Yükle & Otomatik Oyna*
- Gelişmiş Filtreleme Çekmecesi:
  - Yüzdesel Formül, Kapsamlı Formül, Şans Gele, Düz Oynama, Süper Yedili.
- Sayaç ve Bilgilendirme Kartı:
  - "Sitemizde ve yasal bahis sitelerinde kuponlarınızı otomatik oynamak için son güne bırakmayınız."
  - Toplam otomatik oynanan kupon sayacı.

### B. 15 Maçlık Analiz ve Seçim Bülteni (Ekran 3 Referansı)
- Haftalık 15 maç listesi (Örn: Fransa - Türkiye, Bulgaristan - İzlanda vb.).
- Her maç için 1 - Beraber (X) - 2 butonları.
- Buton altlarında kamuoyu ve istatistiksel yüzde dağılımları (Örn: %79, %81, %10, %11...).
- Hızlı butonlar: "Tümüne Favori Seç", "Tümünü Çifte Şans Yap", "Sürprizleri Ekle", "Filtreleri Uygula".

### C. Canlı Forum ve Topluluk (Ekran 1 Referansı)
- Takım logolu avatar balonları (Fenerbahçe, Galatasaray, Trabzonspor, Alanyaspor vb.).
- Mesaj balonları, kullanıcı unvanları (Örn: CUMHURİYET 👑, TATAR34 ✨).
- Yoruma cevap yazma, kupon görseli veya kodu iliştirme.
- "Şikayet Et" ve etkileşim butonları.

### D. Kupon Detayı ve Nesine Tek Tıkla Oynatma
- Üretilen toplam kolon adedi (örn. 9 kolon, 32 kolon, 128 kolon).
- Kupon toplam tutarı (Kolon başı yasal Spor Toto ücretiyle anlık hesaplama).
- **"Nesine'de Otomatik Oyna" Butonu:**
  - Tek tıkla Nesine uygulamasını açma (yüklü değilse Nesine mobil web Spor Toto sayfasını açma).
  - Kolon kodlarını panoya otomatik kopyalayarak kolay yapıştırma/aktarma sağlama.
  - TXT dosyası olarak dışa aktarma (indirme/paylaşma).

---

## 3. Adım Adım Uygulama Planı

1. **Hazırlık ve Yapılandırma:**
   - `metadata.json` ve `res/values/strings.xml` güncellemesi (Uygulama adı: "Spor Toto Hedef 15").
   - Android manifestinde internet ve paylaşım izinlerinin kontrolü.
   - Tema ve renk paletinin ekran görüntülerine uygun olarak hazırlanması.
2. **Veri Modelleri ve Formül Motorunun Yazılması:**
   - 15 maçlık bülten modeli (`TotoMatch`, `MatchPrediction`).
   - Formül indirgeme algoritmaları (14 garanti N-1, 9 kolonluk matris, filtreleme mantığı).
   - Extra1X2 TXT ve Excel parser yardımcı sınıfları.
3. **Nesine Entegrasyon Yöneticisi:**
   - Kupon kolonlarını Nesine formatına serialize eden modül.
   - Tek tıkla Intent ve Web yönlendirme mantığı.
4. **UI Ekranlarının Oluşturulması:**
   - `MainDashboardScreen`: Görseldeki buton blokları, sayaçlar ve duyurular.
   - `MatchAnalysisScreen`: 15 maçlık yüzdeli seçim tablosu.
   - `ForumScreen`: Canlı mesajlaşma, yorum yazma, takım armaları.
   - `CouponsScreen`: Kolon önizleme, maliyet ve Nesine'de oyna akışı.
   - `ProfileScreen`: Üyelik, takım seçimi ve kullanıcı ayarları.
5. **Derleme ve Doğrulama:**
   - `compile_applet` ile projenin sorunsuz derlendiğinin teyit edilmesi.

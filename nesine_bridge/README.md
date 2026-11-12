# Toto ↔ Nesine (bilgisayar/sunucu Chrome köprüsü)

Bu küçük servis **kullanıcının kendi bilgisayarında veya kendi sunucusunda**
çalışır. Toto Android uygulamasından gelen seçilmiş 15 maçlık kolonları
Nesine'nin resmî [Spor Toto](https://www.nesine.com/sportoto) formundaki
**A, B, C, D** alanlarına aktarır, formdaki seçimleri tekrar okuyup doğrular.

**Önemli sınırlar**

- Bu sürüm bahis oynatmaz. **Hemen Oyna** ve **Onayla** düğmelerine basmaz;
  kullanıcı siteyi kendisi kontrol eder. Sunucu yanıtı da bahis
  oynanmadığını açıkça belirtir.
- A–D alanlarının her biri tam bir kolondur. Bir istekte en fazla dört kolon
  aktarılır. 120 veya 350 filtrelenmiş kolon dört kupona dönüştürülmez; Android
  uygulaması bunları ayrı, dört kolonluk gruplar hâlinde yollar.
- Kullanıcı adı/şifreyi **yalnızca Nesine'nin Chrome penceresine** girin.
  Toto uygulaması ve servis bunları sormaz ya da saklamaz. Güvenlik kodu,
  CAPTCHA ve iki aşamalı doğrulama kullanıcı tarafından tamamlanır.
- Resmî Nesine sayfası değiştiğinde otomasyon durur; doğrulama başarısızsa
  hiçbir işlem yapılmış sayılmaz.
- Nesine ve Toto bültenlerinin **aynı hafta ve maç sırasını** içerdiğini
  kullanıcı her aktarımda kontrol etmelidir.

## Kurulum (Windows/macOS/Linux; Python 3.11+)

Depo ana dizininden:

    python -m venv .venv
    # Linux/macOS:
    source .venv/bin/activate
    # Windows PowerShell: .venv\Scripts\Activate.ps1
    python -m pip install -r nesine_bridge/requirements.txt
    python -m playwright install chrome

### 1. İlk kez Chrome'da oturum açın

    python -m nesine_bridge login

Görünen **resmî Nesine Chrome** penceresinde kendiniz giriş yapın, varsa
doğrulamaları tamamlayın. Terminale dönüp Enter'a basın. Oturum çerezleri,
yalnızca bu bilgisayarda ayrı bir Chrome profilinde kalır
(~/.toto-nesine-chrome). Başkasının normal Chrome profili kullanılmaz.

### 2. Servisi başlatın

Güçlü ve rastgele bir erişim anahtarı oluşturun; bunu kaybetmeyin.

    python -c "import secrets; print(secrets.token_urlsafe(36))"

Çıktıyı kendi ortamınızda TOTO_NESINE_BRIDGE_TOKEN değişkenine tanımlayın.
Bu anahtarı GitHub'a, ekran görüntülerine veya kaynak koda eklemeyin.

    python -m nesine_bridge serve

Servis varsayılan olarak yalnızca **127.0.0.1:8765** üzerinde dinler ve
Chrome'u *headless* (arka planda) çalıştırır. Bağlantı kimlik doğrulaması
HTTP Authorization Bearer erişim anahtarıyla yapılır.

**Önemli:** Arka plandaki Chrome penceresinde son oynama onayı verilemez.
Kuponu resmî sayfada kendiniz tamamlamak istiyorsanız aktarım **öncesinde**
servisi görünür modda başlatın (Linux/macOS: TOTO_NESINE_HEADLESS=0;
Windows PowerShell: ortam değişkenini 0 yapın). Görünür modda dahi servis
Hemen Oyna/Onayla düğmelerine sizin yerinize basmaz. Arka plan modu yalnızca
form doldurma ve ekran önizlemesi içindir.

### 3. Telefonu bağlayın

Android uygulamasında **Kuponlar → Nesine → Sunucu Chrome** ekranına
ulaşıp kendi sunucunuzun **HTTPS** adresini ve erişim anahtarını yazın.

Telefondan bilgisayardaki localhost adresine doğrudan erişilemez.
Bu nedenle kendi kontrolünüzde bir **TLS/HTTPS ters vekili veya güvenli
özel ağ tüneli** kurmanız gerekir. Uygulama düz HTTP bağlantılarını
şifre ve kupon verisi sızıntısı riski nedeniyle reddeder. Sunucuyu doğrudan
kimlik doğrulamasız internete açmayın. Tek kullanıcılıdır; workers=1
olmalıdır. Sunucuda Chrome profilinin başka süreçte açık olmaması gerekir.

### 4. Çalışma akışı

1. Toto'dan filtrelenmiş kuponu ve aktarılacak dört kolonluk grubu seçin.
2. İki bültendeki 15 maçın sırasını ve haftasını gözle karşılaştırın.
3. **Chrome'da Seçimleri Doldur** düğmesine basın.
4. Sunucu her 1/X/2 işaretini Nesine formundan tekrar okuyarak doğrular.
5. İsterseniz token korumalı GET /v1/nesine/preview.png ile ekranı
   görüntüleyin. Son oynama onayı bu köprünün kapsamı dışındadır.

## API

- GET /v1/nesine/status: Chrome oturumu ve son aktarım.
- POST /v1/nesine/fill: 1–4 kolonluk JSON gövdesi; aynı hafta onayı zorunlu.
- GET /v1/nesine/preview.png: Korunan ekran görüntüsü.

Tüm uç noktalar erişim anahtarı ister ve Cache-Control: no-store döner.
Servis hiçbir kullanıcı adı, parola veya bahis ödeme bilgisi kabul etmez.

## Testler

    python -m unittest discover -s tests -p 'test_nesine_bridge.py'

Bu testler yalnızca yerel sahte form üzerinde çalışır; Nesine'ye giriş yapmaz
ve gerçek kupon göndermez.

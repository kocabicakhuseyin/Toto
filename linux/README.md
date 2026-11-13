# Linux sunucu kurulumu (Toto ↔ Nesine Chrome köprüsü)

Bu kurulum, **Ubuntu/Debian tabanlı ve systemd kullanan size ait bir Linux
sunucu** içindir. Sunucuda grafik masaüstü bulunması gerekmez.

Sunucuda üç parça çalışır:

1. Chrome/Playwright, resmî Nesine Spor Toto formunu doldurur.
2. FastAPI, yalnızca 127.0.0.1:8765 adresinde çalışır.
3. Tailscale Serve, Android telefonunuza özel ağ içinde HTTPS bağlantı sağlar.

Nesine üyelik bilgileri Android uygulamasına ve API'ye girilmez. İlk Nesine
girişi doğrudan resmî Chrome ekranında yapılır. Bu sürüm **bahis göndermez**;
yalnızca seçimleri doldurur, doğrular ve ekran önizlemesi sunar.

## 1. Kaynak dosyalarını Linux sunucuya alın

Sunucunuza normal SSH kullanıcı hesabınızla bağlanın:

    git clone -b feature/nesine-chrome-bridge https://github.com/kocabicakhuseyin/Toto.git
    cd Toto
    bash linux/01-kur-ubuntu.sh

Kurulum Python 3.11+, Playwright, Google Chrome ve ilk giriş için geçici
Xvfb/noVNC araçlarını hazırlar. Gerekli Ubuntu paketlerini sudo ile kurar.
Ubuntu/Debian dışındaki sistemlerde dağıtıma özel komutlar gerekir.

## 2. Sunucuda Nesine'ye ilk defa giriş yapın (SSH ile)

Sunucudaki ilk SSH oturumunda:

    bash linux/02-giris-ssh.sh

Ekranda bir kerelik VNC parolası ve SSH tünel yönergeleri görünür.

Bilgisayarınızda ikinci bir terminal açıp **kendi sunucu adresinizi** kullanın:

    ssh -N -L 6080:127.0.0.1:6080 KULLANICI@SUNUCU_IP

Ardından bilgisayarınızın Chrome tarayıcısında şu adresi açın:

    http://127.0.0.1:6080/vnc.html

VNC parolasını girin. İçeride görünen resmî Nesine sayfasında üyeliğinize
**kendiniz** giriş yapın. CAPTCHA, güvenlik kodu veya iki aşamalı doğrulama
varsa normal şekilde tamamlayın.

Sunucudaki ilk SSH terminaline dönerek Enter tuşuna basın. Geçici VNC
bağlantısı kapanır; Chrome oturumu aynı Linux kullanıcısının ayrı
profilinde kalır. 5901/6080 portlarını güvenlik duvarından **asla
internete açmayın**. VNC bağlantısı yalnızca SSH tüneli içindir.

## 3. Arka plan servisini kurun

Sunucuda aynı normal Linux kullanıcısıyla:

    bash linux/03-systemd-kur.sh

Bu betik, kullanıcıya özel systemd servisini oluşturup başlatır.
Sunucu erişim anahtarını kullanıcıya ait ve sadece onun okuyabildiği
bir dosyada tutar. Betik sonunda anahtarı yalnızca terminalde gösterir;
**kimseyle paylaşmayın**.

Servis durumunu kontrol edin:

    systemctl --user status toto-nesine-bridge.service

Sunucu yeniden başlatıldığında da otomatik başlasın isterseniz:

    sudo loginctl enable-linger KULLANICI

Buradaki KULLANICI yerine gerçek Linux kullanıcı adınızı yazın.

## 4. Android telefonla güvenli HTTPS bağlantısı

1. Sunucuya [Tailscale Linux](https://tailscale.com/docs/install/linux)
   kurun ve sunucuda **sudo tailscale up** ile kendi hesabınıza bağlayın.
2. Android telefonunuza da Tailscale uygulamasını yükleyip **aynı özel
   Tailscale ağına** giriş yapın.
3. Sunucuda çalıştırın:

       bash linux/04-tailnet-https.sh

4. Betiğin gösterdiği özel HTTPS adresini (örneğin
   https://sunucu.tailnet-adiniz.ts.net) Toto Android uygulamasındaki
   **Kuponlar → Nesine → Chrome Sunucusu** alanına yazın.
5. Üçüncü adımda gösterilen sunucu erişim anahtarını girin.
6. Toto ve Nesine bültenlerinin aynı hafta/maç sırasını içerdiğini
   doğrulayıp en fazla dört kolonluk grubu aktarın.

Bu yöntem **Tailscale Serve** kullanır, herkese açık **Funnel** değil.
Tailscale hesabınızda özel HTTPS sertifikalarının etkinleştirilmesi
istenebilir. Dışarıdan port yönlendirmesi yapılması gerekmez.

## Güvenlik ve çalışma sınırları

- Sunucu açık ve internete bağlı olmalıdır. Servis bir kullanıcı hesabı
  altında çalışır, root değildir.
- Sunucu API'si, Chrome profili ve erişim anahtarı size aittir.
- Nesine web sayfası değişirse form doğrulaması başarısız olup aktarım durur.
- **350 filtreli kolon 4 kolona sıkıştırılmaz**; dörderli gruplar hâlinde
  aktarılır. Arka planda Chrome, gerçek bahis oynama onayı vermez.
- Nesine'ye gerçek para yatırmak veya gerçek kupon onaylamak bu köprünün
  kapsamı dışındadır. Hesabınızda işlem yapıldığını varsaymayın.

Kaynak: [Playwright Chrome tarayıcıları](https://playwright.dev/python/docs/browsers),
[Tailscale Serve](https://tailscale.com/docs/features/tailscale-serve).


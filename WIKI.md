# GraalVM Native Image - İleri Seviye Kullanım Rehberi

[[_TOC_]]

Bu doküman, GraalVM Native Image Compiler Dashboard arayüzündeki **Include Resources** ve **Additional Arguments** özelliklerinin nasıl kullanılacağına dair detaylı teknik bilgiler içerir.

## 1. Kaynak Dosyaların (HTML, CSS, Resim vb.) .exe İçine Gömülmesi (Include Resources)

GraalVM, JAR dosyanızı `.exe` formatına dönüştürürken varsayılan olarak yalnızca derlenmiş Java `.class` dosyalarını (bytecode) dahil eder. Projenizdeki HTML, CSS, resim, JSON veya özellik (properties) dosyalarını gereksiz yük olarak görüp siler. Bu durum, kaynak dosyalarınızın `.exe` çalıştığında bulunamamasına yol açar.

### Çözüm
Arayüzdeki **"Include Resources (Regex):"** kutusunu kullanarak GraalVM'e hangi klasör ve dosyaları tutması gerektiğini söyleyebilirsiniz. Bu kutu bir *Düzenli İfade (Regular Expression)* kabul eder.

**Örnekler:**
- `public/.*` -> `public` klasörünün içindeki tüm dosyaları dahil eder.
- `.*\.css$` -> Projedeki tüm CSS dosyalarını dahil eder.
- `public/.*|.*\.properties$` -> Hem `public` klasörünü hem de `.properties` dosyalarını dahil eder.

> **⚠️ DİKKAT:** Dosyalar `.exe`'ye gömüldüğü için artık fiziksel klasörde bulunmazlar. Bu dosyaları okurken Java içinde `new File(...)` yerine `getClass().getResourceAsStream("/public/index.html")` gibi `ClassLoader` yöntemlerini kullanmalısınız.

---

## 2. Gelişmiş Parametreler ve Argümanlar (Additional Arguments)

Arayüzdeki **"Additional Arguments"** alanı, GraalVM `native-image` aracına komut satırından geçirilebilecek her türlü parametreyi destekler. Birden fazla parametreyi aralarında **birer boşluk** bırakarak yan yana yazabilirsiniz.

**Örnek Kullanım:**
```bash
--no-fallback -J-noverify -R:-Djava.library.path=natives --enable-http
```

### Argüman Türleri ve Önekleri (Prefixes)

GraalVM'de argümanlar ne zaman ve nerede çalışacaklarına göre öneklere ayrılır:

#### A. Çalışma Zamanı Argümanları (Runtime)
Oluşturulan `.exe` dosyasının belirli özelliklerle veya sistem değişkenleriyle çalışmasını istiyorsanız, bu argümanları doğrudan `.exe`'yi çalıştırırken komut satırından verirsiniz. GraalVM build aşamasında çalışma zamanı değişkenleri gömülmez.

* **Kullanım:** `uygulamam.exe -D<property>=<value>`
* **Örnek:** `AESA_View.exe -Djava.library.path=natives` (Bu parametre, uygulamanız her çalıştığında sistem kütüphanelerini 'natives' klasöründen aramasını sağlar).

> **💡 NOT:** `-R:` argümanları (örneğin `-R:MaxHeapSize=...`) sadece GraalVM'in SubstrateVM motoruna özel çok düşük seviye bellek ayarları için kullanılır. Normal sistem değişkenleri (`-D`) için `-R:` öneki KULLANILMAZ. Eğer arayüze `-R:-D...` yazarsanız derleme hata verecektir.

#### B. Derleyici JVM Argümanları (Build-Time JVM - `-J`)
`native-image` aracının kendisi de Java üzerinde çalışır. Eğer derleme işlemi sırasında arka plandaki JVM'e özel bellek veya çalışma ayarları vermek isterseniz `-J` önekini kullanırsınız. (Örneğin arayüzdeki `Max Build RAM` ayarı arka planda `-J-Xmx8G` şeklinde bu öneki kullanır).

* **Kullanım:** `-J<jvm-argümanı>`
* **Örnek:** `-J-noverify` veya `-J-Djava.library.path=natives`

#### C. Standart Native Image Argümanları
GraalVM'in derleme sürecini yapılandıran ana komutlardır.

* **Örnekler:**
  * `--no-fallback` : Eğer native image tam olarak oluşturulamazsa normal JVM üzerinden çalışacak bir "fallback" imajı oluşturmasını engeller (Sadece native imaj üretilmesini zorlar).
  * `--enable-http` / `--enable-https` : HTTP/HTTPS bağlantı özelliklerini etkinleştirir.

---

## 3. Sık Sorulan Sorular

**❓ Soru:** `-noverify` parametresini doğrudan ürettiğim `.exe`'ye verebilir miyim?
**Cevap:** Hayır. `-noverify` Java Sanal Makinesi (JVM) içindeki bytecode doğrulaması (verification) işlemi içindir. GraalVM çıktısı olan `.exe` dosyası bytecode çalıştırmaz, doğrudan makine kodu çalıştırır. Bu nedenle çalışma anında JVM tabanlı komutlar geçersizdir.

**❓ Soru:** GraalVM kodlarımı `obfuscate` (şifreler/karıştırır) eder mi?
**Cevap:** GraalVM ProGuard gibi geleneksel bir obfuscator değildir. Sınıf ve değişken isimlerini `a, b, c` şeklinde şifrelemez. Ancak Java Bytecode'unuzu doğrudan Makine Koduna (Machine Code / Assembly) çevirdiği ve gereksiz metadata/reflection bilgilerini budadığı için, geleneksel bir `.jar` dosyasını kırmaya kıyasla tersine mühendisliği (reverse engineering) inanılmaz derecede zorlaştırır. Dolaylı olarak çok daha güçlü bir "obfuscate" etkisi yaratır.

---

## 4. Geliştirici Araçları: Tracing Agent (İzleme Ajanı)

GraalVM'in en büyük zorluğu `Reflection`, `JNI`, `Dynamic Proxies` ve `Resources` (HTML/CSS) gibi çalışma zamanında dinamik olarak çağrılan kodlardır. Normal şartlarda GraalVM bu kodları göremez ve `.exe` içine dahil etmez.

Bunu çözmek için arayüze **"Run Tracing Agent"** butonu eklenmiştir.

### Nasıl Kullanılır?
1. Hedef JAR dosyanızı ve GraalVM klasörünüzü seçin.
7. **Run Tracing Agent** butonuna tıklayın.
8. Uygulamanız Java üzerinden başlatılır. *Lütfen uygulamanızı kullanın (butonlara basın, sayfaları gezin) ve ardından normal şekilde kapatın.*
9. Uygulama kapandığında, JAR dosyanızın bulunduğu dizinde `native-image-configs` adında bir klasör oluşturulur.
10. Arayüz otomatik olarak `-H:ConfigurationFileDirectories=native-image-configs` parametresini argümanlarınıza ekler.
11. Artık tek yapmanız gereken **Build Native Image** tuşuna basmaktır. Tüm eksik reflection ve JNI konfigürasyonları otomatik olarak derlemeye dahil edilir!

### Merge Agent Configs (Ayarları Birleştirme)
Eğer çok büyük bir projeniz varsa, her menüyü tek seferde test etmek imkansızdır. Arayüzdeki **Merge Agent Configs** özelliği sayesinde:
* Agent'ı ilk çalıştırdığınızda (Örn: Giriş Ekranını test edin), config dosyanız oluşur.
* Agent'ı ikinci çalıştırdığınızda (Örn: Rapor Ekranını test edin), yeni bulduğu reflection kayıtlarını eski dosyayı **silmeden üzerine ekleyerek birleştirir**.
* Böylece testlerinizi parça parça yaparak uygulamanızın eksiksiz bir reflection haritasını çıkarabilirsiniz.

---

## 5. Reachability Metadata & Auto-Detect

Eğer uygulamanız Gson, Hibernate, JOGL, Netty gibi popüler üçüncü parti kütüphaneler (frameworkler) kullanıyorsa, Tracing Agent ile saatlerce uğraşmanıza gerek kalmayabilir.

GraalVM'in devasa **Reachability Metadata Repository** veritabanı doğrudan bu arayüzün içine offline (airgapped) çalışacak şekilde gömülmüştür.

### Nasıl Kullanılır?
1. **Target JAR** dosyanızı seçin.
2. Arayüzdeki **Reachability Metadata** butonuna tıklayın.
3. Açılan pencerede **Extract Offline Repository** diyerek veritabanını çıkarın.
4. Yazılımımız, Target JAR dosyanızı bir röntgen cihazı gibi tarayarak içindeki `pom.properties` dosyalarını okur ve kullandığınız frameworkleri milisaniyeler içinde tespit edip listeler (**Auto-Detect**).
5. **Apply Selected** diyerek bu kütüphanelerin uyumlu klasörlerini out-of-the-box çalışacak şekilde derlemeye otomatik dahil edebilirsiniz.

---

## 6. Hızlı Parametreler (Quick Flags)

Arayüzde bulunan **Quick Flags** bölümündeki onay kutucukları, en sık kullanılan `native-image` parametrelerini tek tıkla aktif etmenizi sağlar:

* **Enable HTTPS:** Web istekleri (`https://`) için gerekli olan SSL kütüphanelerini ekler (`--enable-https --enable-http`).
* **Static Build:** Uygulamayı `%100` statik derler (`--static`). Hedef sistemde VCRUNTIME kütüphanesi olmasa bile çalışmasını garanti eder.
* **Verbose:** Derleme sırasında arka planda neler olup bittiğini tüm detaylarıyla gösterir (`--verbose`).
* **Exit Handlers:** İşletim sisteminden gelen kapatma (SIGTERM vb.) sinyallerinin uygulamanız tarafından yakalanmasını sağlar (`--install-exit-handlers`).
* **Diagnostics:** Derleme işlemi çökerse, GraalVM ekibine hata raporu gönderebilmeniz için detaylı dump dosyaları (`--diagnostics-mode`) oluşturur.
* **Merge Agent Configs:** Tracing Agent çalıştırıldığında eski konfigürasyonları ezmek yerine yeni bulunanları üstüne ekler.
* **Package as Standalone:** Swing ve AWT tabanlı projelerin font/kütüphane hatası vermemesi için GraalVM DLL ve lib dosyalarını projenin yanına `.bat` başlatıcısıyla beraber kopyalar.

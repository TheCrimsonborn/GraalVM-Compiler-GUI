# GraalVM Native Image Compiler Dashboard

GraalVM kullanarak Java uygulamalarınızı (JAR dosyalarını) Windows üzerinde yerel çalıştırılabilir dosyalara (`.exe`) dönüştürmeyi (`native-image` sürecini) otomatikleştiren, görselleştiren ve kolaylaştıran modern bir Java Swing kullanıcı arayüzüdür. Komut satırı karmaşasını ortadan kaldırarak uzun süren derleme süreçlerini kolayca yönetmenizi sağlar.

##  Özellikler

*   **Modern ve Şık Arayüz:** FlatLaf kullanılarak tasarlanmış göz yormayan, karanlık tema destekli (Dark Mode) modern masaüstü arayüzü.
*   **Gerçek Zamanlı Log Takibi:** `native-image` derleme sürecinin komut satırı çıktılarını (stdout ve stderr) anlık olarak arayüz üzerinden izleyebilme.
*   **Kolay Konfigürasyon:** Hedef JAR dosyası, GraalVM dizini ve Visual Studio Build Tools (`vcvars64.bat`) yollarını dosya seçici ile arayüzden kolayca belirleyebilme.
*   **Bellek ve Argüman Yönetimi:** Derleme işlemi için ayrılacak maksimum RAM miktarını (örn: 8GB, 12GB, 16GB vb.) açılır menüden seçebilme ve özel `native-image` argümanlarını (örn: `--no-fallback`) girebilme imkanı.
*   **Kurumsal Ağ Uyumluluğu:** `build_project.bat` içerisindeki özel Maven yapılandırması sayesinde, kurumsal ağlardaki katı SSL/TLS sertifika kısıtlamalarına takılmadan (SSL Bypass) projenin sorunsuz derlenebilmesi.
*   **Asenkron Arka Plan İşlemleri:** Derleme süreci Java ProcessBuilder ile ayrı bir thread'de yönetilir, böylece uzun süren derleme işlemlerinde arayüz (UI) kilitlenmez ve duyarlı kalmaya devam eder.
*   **Otomatik Konfigürasyon Üretici (Tracing Agent):** GraalVM'in en çok zorlandığı Reflection ve JNI gibi yapıları algılamak için hedef uygulamanızı özel bir ajanla (native-image-agent) çalıştırır ve gerekli JSON konfigürasyon dosyalarını otomatik üretir.
*   **Merge Agent Configs:** Tracing Agent ile yapılan parça parça testlerde, her testin sonucunu silmek yerine eski dosyaların üzerine akıllıca birleştirerek (merge) uygulamanızın eksiksiz bir reflection haritasını çıkarmanızı sağlar.
*   **Offline Reachability Metadata & Auto-Detect:** İnternet erişimi olmayan (airgapped) bilgisayarlarda bile GraalVM Reachability Metadata deposunu kullanın. Uygulamanızın JAR dosyasını X-Ray gibi tarayıp içindeki frameworkleri (Gson, Hibernate, JOGL) otomatik tespit eder ve uyumlu klasörleri derlemeye out-of-the-box olarak ekler.
*   **Proje Kaydetme ve Yükleme (.graalproj):** Her derlemede onlarca parametreyi baştan girmek yerine, `File -> Save Project` ile tüm ayarlarınızı diske kaydedip sonradan tek tıkla yükleyebilirsiniz. Projeler derlemeden önce `last-build.graalproj` dosyasına otomatik yedeklenir.
*   **Hızlı Parametre Seçimi (Quick Flags):** Sık kullanılan `--enable-https`, `--static`, `--verbose` gibi karmaşık `native-image` parametrelerini onay kutuları (checkbox) aracılığıyla tek tıkla seçebilme.
*   **Kaynak Dosya Gömme (Include Resources):** HTML, CSS, resim gibi dosyaları arayüz üzerinden düzenli ifade (Regex) belirterek üretilen `.exe` dosyasının hafızasına güvenle dahil edebilme.

##  Sistem Gereksinimleri

Bu projenin çalıştırılabilmesi ve native executable (.exe) üretebilmesi için aşağıdaki gereksinimlerin sistemde bulunması gereklidir:

1.  **Java 11 veya üzeri** (Kullanıcı arayüzünü çalıştırmak için).
2.  **Maven** (Projeyi derlemek için).
3.  **GraalVM CE Java 11 (veya uyumlu sürüm)**: `native-image` aracı kurulu olmalıdır.
4.  **Microsoft Visual Studio 2022 Build Tools** (veya eşdeğeri C++ derleyici araç setleri): GraalVM'in Windows üzerinde native image oluşturabilmesi için C++ derleyicisine ihtiyacı vardır. İşlem başlamadan önce `vcvars64.bat` scripti çağrılarak ortam değişkenleri ayarlanır.

##  Proje Yapısı

```text
GraalVM11/
├── build_project.bat               # Projeyi derlemek (SSL Bypass ile) için otomatik Maven betiği.
├── start_dashboard.bat             # Derlenmiş UI uygulamasını arka planda (javaw) başlatan betik.
├── graalvm-ce-java11-22.3.3/       # (Varsayılan) GraalVM kurulum dizini.
├── graal-compiler-ui/              # Ana Maven proje dizini.
│   ├── pom.xml                     # Maven konfigürasyon dosyası (FlatLaf bağımlılığı ve Fat JAR ayarı içerir).
│   └── src/main/java/com/graalwrapper/
│       ├── App.java                # Uygulamanın ana giriş noktası.
│       ├── GraalCompilerDashboard.java # Arayüzün (Swing) tasarlandığı ve mantığının bulunduğu sınıf.
│       └── NativeImageExecutor.java    # CMD üzerinden arka planda native-image komutunu çalıştıran sınıf.
```

##  Kullanım Adımları

### 1. Projeyi Derleme (Build)
Uygulamayı kullanmadan önce Java kaynak kodlarının derlenip paketlenmesi gerekmektedir.
- Proje ana dizinindeki **`build_project.bat`** dosyasına çift tıklayın.
- Bu işlem projeyi derleyecek ve `graal-compiler-ui\target\` dizini altında çalıştırılabilir bir **Fat JAR** (`graal-compiler-ui-jar-with-dependencies.jar`) oluşturacaktır.

### 2. Dashboard'u Başlatma
- Derleme işlemi tamamlandıktan sonra **`start_dashboard.bat`** betiğine çift tıklayarak arayüzü başlatabilirsiniz.
- *Not: Betik konsol penceresini gizlemek adına `javaw` kullanmaktadır.*

### 3. Native Image Oluşturma (.exe Çıktısı Alma)
Arayüz açıldığında aşağıdaki adımları izleyin:
1.  **Target JAR File:** Derleyerek `.exe`'ye dönüştürmek istediğiniz hedef Java uygulamasının (JAR) yolunu *Browse...* diyerek seçin.
2.  **GraalVM Home:** Bilgisayarınızdaki GraalVM kurulumunun kök dizinini seçin (örn: `.\graalvm-ce-java11-22.3.3`).
3.  **vcvars64.bat Path:** Sisteminizdeki Visual Studio Build Tools kurulum dizininden `vcvars64.bat` dosyasının doğru yolunu belirtin. *(Genellikle: C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat)*
4.  **Additional Arguments:** Gerekirse `--no-fallback`, `-O3` gibi GraalVM özel parametrelerini buraya girebilirsiniz.
5.  **Include Resources (Regex):** Uygulamanızın içine gömmek istediğiniz statik dosyalarınız varsa (örn: HTML, CSS), klasörünüzün regex desenini yazın (örn: `public/.*`).
6.  **Quick Flags:** HTTPS desteği, statik derleme, **Merge Agent Configs** gibi özel bayrakları onay kutuları aracılığıyla kolayca aktif edin.
7.  **Reachability Metadata:** Eğer uygulamanız 3. parti kompleks kütüphaneler kullanıyorsa (JOGL, Gson vb.) sağ alttaki `Reachability Metadata` butonuna basın, `Extract Offline Repository` diyerek veritabanını dışarı çıkarın ve yazılımın JAR dosyanızı tarayıp bulduğu bağımlılıkları `Apply Selected` diyerek otomatik konfigüre etmesine izin verin.
8.  **Max Build RAM:** Native image derleme süreci çok fazla RAM tüketir. Sisteminizin belleğine uygun olan en yüksek miktarı seçmeniz derleme süresini kısaltacaktır.
9.  **Tracing Agent (İsteğe Bağlı):** Uygulamanız Reflection/JNI kullanıyorsa **Run Tracing Agent** butonuna basarak programınızı test edin. Arayüz eksik konfigürasyon dosyalarını sizin için otomatik oluşturacaktır!
10. **Build Native Image** butonuna tıklayın. İşlem başlayacak ve derleme detayları anlık olarak alt kısımdaki *Build Console* ekranına yansıyacaktır. İşlem bitiminde hedef JAR dosyanızın bulunduğu dizinde .exe dosyanız hazır olacaktır.

##  Teknik Detaylar

*   **SSL Bypass:** Şirket ağlarında karşılaşılan Maven SSL sertifika hatalarını aşmak için `build_project.bat` içerisinde `maven.wagon.http.ssl.insecure=true` gibi özel argümanlar kullanılmıştır.
*   **Compound Command Execution:** Windows üzerinde GraalVM derlemesi yaparken C++ build ortamının hazırlanması şarttır. `NativeImageExecutor.java` sınıfı, `cmd.exe /c` üzerinden zincirleme (compound) komut çalıştırarak (önce `vcvars64.bat` çağrısı, ardından `native-image.cmd` çağrısı) bu gereksinimi şeffaf bir şekilde karşılar.

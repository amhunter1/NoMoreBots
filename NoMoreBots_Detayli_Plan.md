# NoMoreBots Plugin - Detaylı Plan

## 1. Proje Yapısı

### Maven Proje Yapısı
```
NoMoreBots/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── melut/
│       │           └── nomorebots/
│       │               ├── NoMoreBotsPlugin.java
│       │               ├── config/
│       │               │   ├── ConfigManager.java
│       │               │   ├── LanguageManager.java
│       │               │   └── Settings.java
│       │               ├── database/
│       │               │   ├── DatabaseManager.java
│       │               │   ├── PlayerData.java
│       │               │   └── SQLiteHandler.java
│       │               ├── verification/
│       │               │   ├── VerificationManager.java
│       │               │   ├── VerificationSession.java
│       │               │   └── CaptchaGenerator.java
│       │               ├── limbo/
│       │               │   ├── LimboManager.java
│       │               │   ├── LimboServerHandler.java
│       │               │   └── GuiPacketHandler.java
│       │               ├── events/
│       │               │   ├── PlayerConnectionHandler.java
│       │               │   └── ServerSwitchHandler.java
│       │               ├── commands/
│       │               │   ├── AdminCommands.java
│       │               │   └── VerificationCommands.java
│       │               └── utils/
│       │                   ├── TimeoutManager.java
│       │                   ├── MessageUtils.java
│       │                   └── ItemUtils.java
│       └── resources/
│           ├── velocity-plugin.json
│           ├── config.yml
│           └── lang/
│               ├── tr.yml
│               └── en.yml
```

## 2. Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Velocity API -->
    <dependency>
        <groupId>com.velocitypowered</groupId>
        <artifactId>velocity-api</artifactId>
        <version>3.3.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- SQLite -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.44.1.0</version>
    </dependency>
    
    <!-- NanoLimbo API -->
    <dependency>
        <groupId>ua.nanit</groupId>
        <artifactId>limboapi</artifactId>
        <version>1.1.9</version>
    </dependency>
    
    <!-- Configuration -->
    <dependency>
        <groupId>org.spongepowered</groupId>
        <artifactId>configurate-yaml</artifactId>
        <version>4.1.2</version>
    </dependency>
</dependencies>
```

## 3. Ana Config Dosyası (config.yml)

```yaml
# NoMoreBots Plugin Konfigürasyon
version: 1.0

# Genel Ayarlar
general:
  language: "tr" # Desteklenen: tr, en
  debug: false
  plugin-prefix: "&8[&6NoMoreBots&8] "

# Veritabanı Ayarları
database:
  type: "sqlite" # sqlite, mysql
  sqlite:
    file: "plugins/NoMoreBots/data.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "nomorebots"
    username: "root"
    password: "password"
    connection-pool-size: 5

# Limbo Sunucu Ayarları
limbo:
  enabled: true
  host: "127.0.0.1"
  port: 25566
  auto-start: true
  max-players: 100
  brand-name: "&6NoMoreBots &7Verification"
  
# Bot Doğrulama Sistemi
verification:
  enabled: true
  
  # GUI Ayarları
  gui:
    title: "&c&l%target_item% &f&litemine tıkla!"
    size: 54 # 6x9 inventory
    fill-empty-slots: true
    empty-slot-item: "BLACK_STAINED_GLASS_PANE"
    
    # Random items listesi
    random-items:
      - "DIAMOND"
      - "EMERALD"
      - "IRON_INGOT"
      - "GOLD_INGOT"
      - "REDSTONE"
      - "COAL"
      - "APPLE"
      - "BREAD"
      - "STONE"
      - "WOOD"
      - "GLASS"
      - "OBSIDIAN"
    
    # Target items (bunlardan biri random seçilir)
    target-items:
      - "DIAMOND"
      - "EMERALD"
      - "IRON_INGOT"
      - "GOLD_INGOT"
      - "REDSTONE"
    
    # Item sayıları
    total-items: 15 # GUI'de toplam kaç item olacak
    decoy-items: 12 # Hedef dışındaki item sayısı
    
  # Deneme Ayarları
  attempts:
    max-attempts: 3
    reset-on-success: true
    
  # Timeout Ayarları
  timeout:
    duration: 600 # saniye (10 dakika)
    persistent: true # Sunucu yeniden başladığında korunur
    
  # Başarı Ayarları
  success:
    target-server: "lobby" # Başarılı doğrulama sonrası gidilecek sunucu
    remember-duration: 86400 # 24 saat (saniye)
    
# Bypass Sistemi
bypass:
  permission: "nomorebots.bypass"
  ip-whitelist:
    - "127.0.0.1"
    - "localhost"
  enabled: true
  
# Admin Komutları
commands:
  admin-permission: "nomorebots.admin"
  aliases:
    - "nmb"
    - "botcheck"
    
# Performans Ayarları
performance:
  async-database: true
  cleanup-interval: 3600 # saniye
  max-sessions: 500
  session-timeout: 300 # saniye
  
# Güvenlik Ayarları
security:
  max-verification-time: 120 # saniye
  anti-spam-delay: 1000 # milisaniye
  log-attempts: true
```

## 4. Dil Dosyaları

### Türkçe (lang/tr.yml)
```yaml
messages:
  prefix: "&8[&6NoMoreBots&8] "
  
  verification:
    required: "&cBot doğrulaması gerekli! Lütfen bekleyin..."
    gui-title: "&c&l%target_item% &f&litemine tıklayın!"
    click-target: "&eDoğru item: &f%item%"
    wrong-item: "&cYanlış item seçtiniz! Kalan deneme: &e%attempts%"
    success: "&aBot doğrulaması başarılı! Ana sunucuya yönlendiriliyorsunuz..."
    failed: "&c3 kere yanlış deneme yaptınız!"
    timeout: "&cBot doğrulaması başarısız! &e%time% &cdakika sonra tekrar deneyebilirsiniz."
    already-verified: "&aZaten bot doğrulamasından geçmişsiniz!"
    session-expired: "&cDoğrulama süresi doldu! Tekrar deneyin."
    
  admin:
    reload-success: "&aKonfigürasyon başarıyla yeniden yüklendi!"
    reload-failed: "&cKonfigürasyon yeniden yüklenirken hata oluştu!"
    player-verified: "&a%player% oyuncusu manuel olarak doğrulandı!"
    player-reset: "&a%player% oyuncusunun doğrulama durumu sıfırlandı!"
    player-timeout: "&a%player% oyuncusuna %duration% saniyelik timeout verildi!"
    player-not-found: "&cBelirtilen oyuncu bulunamadı!"
    bypass-added: "&a%player% oyuncusuna bypass yetkisi verildi!"
    bypass-removed: "&a%player% oyuncusundan bypass yetkisi kaldırıldı!"
    
  stats:
    header: "&6=== NoMoreBots İstatistikleri ==="
    total-players: "&eTopla Oyuncu: &f%count%"
    verified-players: "&aDoğrulanmış Oyuncular: &f%count%"
    timeout-players: "&cTimeout'lu Oyuncular: &f%count%"
    active-sessions: "&bAktif Oturumlar: &f%count%"
    
  errors:
    no-permission: "&cBu komutu kullanma yetkiniz yok!"
    unknown-command: "&cBilinmeyen komut! &e/nomorebots help &ckullanın."
    database-error: "&cVeritabanı hatası oluştu!"
    limbo-error: "&cLimbo sunucu bağlantı hatası!"
```

### İngilizce (lang/en.yml)
```yaml
messages:
  prefix: "&8[&6NoMoreBots&8] "
  
  verification:
    required: "&cBot verification required! Please wait..."
    gui-title: "&c&lClick on the &f&l%target_item%!"
    click-target: "&eTarget item: &f%item%"
    wrong-item: "&cWrong item selected! Remaining attempts: &e%attempts%"
    success: "&aBot verification successful! Redirecting to main server..."
    failed: "&cYou failed 3 verification attempts!"
    timeout: "&cBot verification failed! Try again in &e%time% &cminutes."
    already-verified: "&aYou have already passed bot verification!"
    session-expired: "&cVerification session expired! Please try again."
    
  admin:
    reload-success: "&aConfiguration reloaded successfully!"
    reload-failed: "&cFailed to reload configuration!"
    player-verified: "&aPlayer %player% has been manually verified!"
    player-reset: "&aPlayer %player%'s verification status has been reset!"
    player-timeout: "&aPlayer %player% has been given a %duration% second timeout!"
    player-not-found: "&cSpecified player not found!"
    bypass-added: "&aBypass permission added to %player%!"
    bypass-removed: "&aBypass permission removed from %player%!"
    
  stats:
    header: "&6=== NoMoreBots Statistics ==="
    total-players: "&eTotal Players: &f%count%"
    verified-players: "&aVerified Players: &f%count%"
    timeout-players: "&cTimed-out Players: &f%count%"
    active-sessions: "&bActive Sessions: &f%count%"
    
  errors:
    no-permission: "&cYou don't have permission to use this command!"
    unknown-command: "&cUnknown command! Use &e/nomorebots help&c."
    database-error: "&cDatabase error occurred!"
    limbo-error: "&cLimbo server connection error!"
```

## 5. Veritabanı Şeması

### SQLite Tabloları
```sql
-- Oyuncu doğrulama verisi
CREATE TABLE IF NOT EXISTS player_verification (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_verification TIMESTAMP,
    verification_status INTEGER DEFAULT 0, -- 0: pending, 1: verified, 2: timeout
    total_attempts INTEGER DEFAULT 0,
    failed_attempts INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    timeout_until TIMESTAMP NULL,
    remember_until TIMESTAMP NULL,
    bypass_granted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Aktif doğrulama oturumları
CREATE TABLE IF NOT EXISTS verification_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    player_uuid VARCHAR(36),
    player_name VARCHAR(16),
    target_item VARCHAR(50),
    gui_items TEXT, -- JSON format
    attempt_number INTEGER DEFAULT 1,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    completed BOOLEAN DEFAULT FALSE,
    success BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (player_uuid) REFERENCES player_verification(uuid)
);

-- Timeout kayıtları (log amaçlı)
CREATE TABLE IF NOT EXISTS timeout_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid VARCHAR(36),
    player_name VARCHAR(16),
    reason VARCHAR(100),
    duration INTEGER,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Plugin ayarları (runtime)
CREATE TABLE IF NOT EXISTS plugin_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- İndeksler
CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_verification(uuid);
CREATE INDEX IF NOT EXISTS idx_session_player ON verification_sessions(player_uuid);
CREATE INDEX IF NOT EXISTS idx_timeout_player ON timeout_logs(player_uuid);
CREATE INDEX IF NOT EXISTS idx_verification_status ON player_verification(verification_status);
```

## 6. Ana Plugin Sınıfları

### Plugin Ana Sınıfı Yapısı
```java
@Plugin(
    id = "nomorebots",
    name = "NoMoreBots",
    version = "1.0.0",
    authors = {"melut"}
)
public class NoMoreBotsPlugin {
    // Ana plugin mantığı
    // Event listener kayıtları
    // Command kayıtları
    // Manager sınıfları initialization
}
```

### Temel Manager Sınıfları

1. **ConfigManager**: Yapılandırma dosyası yönetimi
2. **DatabaseManager**: SQLite bağlantı ve query yönetimi
3. **VerificationManager**: Bot doğrulama mantığı
4. **LimboManager**: Limbo sunucu entegrasyonu
5. **TimeoutManager**: Oyuncu timeout sistemi
6. **LanguageManager**: Çoklu dil desteği

## 7. Bot Doğrulama Akışı

### Akış Diyagramı
```
Oyuncu Bağlanır
      ↓
IP Whitelist Kontrolü → [Bypass] → Ana Sunucuya Yönlendir
      ↓
Bypass Permission → [Var] → Ana Sunucuya Yönlendir
      ↓
Daha Önce Doğrulandı mı? → [Evet] → Ana Sunucuya Yönlendir
      ↓
Timeout Kontrolü → [Aktif] → Disconnect + Mesaj
      ↓
Limbo Sunucusuna Yönlendir
      ↓
GUI Oluştur (57 slot)
      ↓
Random Items + Target Item Yerleştir
      ↓
Oyuncu Item Seçer
      ↓
Doğru mu? → [Evet] → Başarılı + Ana Sunucuya Yönlendir
      ↓ [Hayır]
Deneme Sayısı Artır
      ↓
3 Deneme Doldu mu? → [Hayır] → GUI'yi Yenile
      ↓ [Evet]
10 Dakika Timeout + Disconnect
```

## 8. Admin Komut Sistemi

### Komut Yapısı
```
/nomorebots help - Yardım menüsü
/nomorebots reload - Config yeniden yükle
/nomorebots verify <player> - Manuel doğrulama
/nomorebots reset <player> - Doğrulama durumunu sıfırla
/nomorebots timeout <player> [süre] - Manuel timeout
/nomorebots bypass <player> - Bypass ver/kaldır
/nomorebots stats - Plugin istatistikleri
/nomorebots cleanup - Eski kayıtları temizle
/nomorebots debug <on/off> - Debug modu
/nomorebots limbo <start/stop/restart> - Limbo sunucu kontrolü
```

## 9. Performans Optimizasyonları

### Veri Tabanı Optimizasyonu
- Connection pooling
- Asenkron database işlemleri
- Batch operations
- İndeks kullanımı
- Otomatik cleanup

### Memory Management
- Session cache sistemi
- Timeout kontrolü için scheduler
- Unused data cleanup
- Configurable cache size

### Network Optimizasyonu
- Packet compression
- Minimal data transfer
- Connection reuse

## 10. Test Senaryoları

### Fonksiyonel Testler
1. **Başarılı Doğrulama Testi**
2. **Başarısız Doğrulama Testi** (3 deneme)
3. **Timeout Testi** (10 dakika)
4. **Bypass Testi** (permission + IP)
5. **Admin Komut Testleri**
6. **Config Reload Testi**
7. **Database Connection Testi**
8. **Limbo Server Connection Testi**

### Performance Testler
1. **Yüksek Oyuncu Sayısı Testi** (100+ eşzamanlı)
2. **Memory Leak Testi**
3. **Database Performance Testi**
4. **Network Latency Testi**

### Edge Case Testler
1. **Database Bağlantı Kopması**
2. **Limbo Server Crash**
3. **Config Dosyası Corruption**
4. **Player Disconnect During Verification**
5. **Server Restart During Verification**

## 11. Güvenlik Özellikleri

### Anti-Exploit
- Session timeout (max 2 dakika)
- Rate limiting (spam koruması)
- IP-based whitelist
- Permission-based bypass
- Audit logging

### Data Protection
- UUID-based identification
- Encrypted timeout data
- Secure database queries
- Input validation
- XSS protection (admin commands)

Bu plan istediğiniz tüm özelliği kapsamakta ve Velocity 3.3+ için optimize edilmiştir. Plan tamamlandı ve implmentasyon için hazırdır.
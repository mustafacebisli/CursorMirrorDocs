# InventoryManager

> **Source:** `src/main/java/com/example/business/InventoryManager.java`
> **Last verified:** `2026-05-10`
> **Status:** `fresh`

## Purpose
SKU bazında stok yönetimini (kayıt, rezervasyon, serbest bırakma, satış kesinleştirme,
yeniden stok) sağlayan **in-memory** ve **synchronized** bir servis. Sipariş akışındaki
"reserve → commit / release" iki aşamalı stok protokolünü uygular ve düşük stok
seviyesinde audit event üretir.

## Public API
- `InventoryManager(AuditLogger, int lowStockThreshold)` — bağımlılık ve düşük stok eşiği enjekte edilir.
- `registerSku(String sku, int initialQuantity) -> void` — yeni SKU oluşturur; SKU zaten kayıtlıysa `IllegalStateException`.
- `reserve(String sku, int quantity) -> void` — stoktan rezerve eder; yetersiz mevcutta `OutOfStockException`.
- `release(String sku, int quantity) -> void` — rezervasyonu çözer; alt sınır `0`'a clamp edilir, hata fırlatmaz.
- `commit(String sku, int quantity) -> void` — rezerve edilmiş miktarı satışa dönüştürür (`onHand` ve `reserved` aynı oranda azalır); rezerveden fazla commit `IllegalStateException`.
- `restock(String sku, int quantity) -> void` — `onHand`'i artırır.
- `get(String sku) -> Optional<StockItem>` — SKU yoksa `Optional.empty()`.
- `OutOfStockException` (nested) — istenen / mevcut miktarları mesajda taşır.

## Dependencies
- `AuditLogger` — her mutasyon ve düşük stok uyarısında log yazar.
- `StockItem` — immutable model; `withOnHand`, `withReserved` gibi `with*` kopya metodlarıyla kullanılır.

## Side effects
- Mutates internal `Map<String, StockItem> stockBySku` (in-memory, kalıcılık yok).
- `auditLogger.log(...)` çağrıları:
  - `inventory.skuRegistered`, `inventory.reserved`, `inventory.released`,
    `inventory.committed`, `inventory.restocked`,
  - `inventory.lowStock` (sadece `reserve` ve `commit` sonrası, eşiğin altındaysa).
- Hiçbir DB / network IO yapılmaz.

## Invariants & gotchas
- **Mutasyon yapan tüm metodlar `synchronized`:** Yazma yolu thread-safe (instance lock). CustomerService'in aksine bu sınıf concurrent yazmaya dayanıklıdır.
- **`get()` `synchronized` DEĞİL:** Eşzamanlı yazma sırasında okuma yapan thread tutarsız değer görebilir veya `HashMap` resize'ı sırasında bozulmuş bir state ile karşılaşabilir. Düzeltmek için ya `get`'i de `synchronized` yapmalı ya da backing store `ConcurrentHashMap` olmalı.
- **`release` defansif değil:** `reserved - quantity` negatife düşerse `Math.max(0, ...)` ile sessizce `0`'a yuvarlanır; çağıran "fazladan release ettim" gerçeğini fark etmez. Geri dönüş tipi `void` olduğu için audit log bile gerçek değişimi değil, *istenen* miktarı taşır.
- **`commit`'in tek koruması `reserved >= quantity`:** `onHand >= reserved` invariantı reserve/restock akışı tarafından korunduğu varsayılır; doğrudan StockItem'ı dışarıdan oluşturup map'e koyacak bir API yok, yani sınıf üzerinden bu invariant kırılmıyor.
- **Negatif miktar validasyonu yok:** `reserve(sku, -5)`, `restock(sku, -5)`, `registerSku(sku, -3)` çağrıları kabul edilir ve sayaçları ters yönde değiştirir. Defensive check eklemek isteniyorsa giriş katmanında veya bu metodların başında yapılmalı.
- **`warnIfLow` asimetrik:** Sadece `reserve` ve `commit` sonrası tetiklenir; `release` ve `restock` stoğu rahatlattığında "lowStock recovered" gibi bir event üretilmez.
- **`lowStockThreshold` runtime'da değişmez:** Constructor'da set edilir, mutator yok.
- **Persistence yok:** Süreç restart'ında tüm SKU'lar kaybolur; bu sınıf prod-ready bir warehouse repository'si değil, in-memory bir prototiptir.
- **`Instant.now()` doğrudan çağrılıyor (`registerSku`):** Zaman testlenebilir değil; deterministik testler için `Clock` enjeksiyonuna ihtiyaç var.
- **`OutOfStockException` checked değil:** `RuntimeException`'dan türer; çağıran tarafa zorunlu yakalama yoktur.

## Related docs
- `.cursor-docs/src/main/java/com/example/business/AuditLogger.md` *(henüz oluşturulmadı)*
- `.cursor-docs/src/main/java/com/example/business/StockItem.md` *(henüz oluşturulmadı)*
- `.cursor-docs/src/main/java/com/example/business/CustomerService.md` — benzer "in-memory + AuditLogger" deseninin uygulandığı kardeş servis.

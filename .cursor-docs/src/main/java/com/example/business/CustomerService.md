# CustomerService

> **Source:** `src/main/java/com/example/business/CustomerService.java`
> **Last verified:** `2026-05-10`
> **Status:** `fresh`


## Purpose
Müşteri yaşam döngüsünü (oluşturma, güncelleme, soft-delete, lookup, e-posta
doğrulama) yöneten in-memory domain servisi. Tüm iş kuralları (e-posta format
kontrolü, duplicate e-posta engelleme, soft-delete sonrası erişim kapatma) burada
toplanır; HTTP / transport katmanı içermez.

## Public API
- `CustomerService(EmailVerificationQueue, AuditLogger)` — bağımlılıkları enjekte eden constructor.
- `create(String fullName, String email) -> Customer` — yeni müşteri oluşturur, doğrulama tokenı kuyruğa atar.
- `update(UUID id, String fullName, String email) -> Customer` — aktif müşteriyi günceller; e-posta değişirse yeniden doğrulama tetikler.
- `softDelete(UUID id) -> void` — `deletedAt` damgalar; kayıt store'da kalmaya devam eder.
- `deactivate(UUID id) -> void` — `softDelete`'in genişletilmiş hâli: `deletedAt` damgalarken aynı zamanda `emailVerifiedAt`'i `null`'a çeker (reaktivasyonda yeniden doğrulama zorunlu).
- `findById(UUID id) -> Optional<Customer>` — yalnızca aktif (silinmemiş) müşteriyi döner.
- `verifyEmail(UUID id, String token) -> Customer` — tokenı tüketip `emailVerifiedAt` set eder; idempotenttir.
- `DuplicateEmailException` (nested) — aktif başka bir müşteri aynı e-postayı kullanıyorsa fırlatılır.
- `CustomerNotFoundException` (nested) — kayıt yoksa veya soft-deleted ise fırlatılır.

## Dependencies
- `EmailVerificationQueue` — `enqueueVerification(id, email)` ve `consumeToken(id, token)` çağrıları için kullanılır.
- `AuditLogger` — her mutasyon sonrası audit event yazar (`customer.created`, `customer.updated`, `customer.softDeleted`, `customer.deactivated`, `customer.emailVerified`).
- `Customer` (domain) — immutable model; `withFullName`, `withEmail`, `withEmailVerifiedAt`, `withDeletedAt` gibi `with*` metodlarıyla kopyalanır.

## Side effects
- Mutates internal `Map<UUID, Customer> store` (in-memory, kalıcı DB yok).
- `emailQueue.enqueueVerification(...)` çağrısı: `create` ve e-posta değiştiren `update` yollarında.
- `emailQueue.consumeToken(...)` çağrısı: `verifyEmail` yolunda (token tüketilir).
- `auditLogger.log(...)` çağrısı: tüm yazma operasyonlarından sonra.
- Hiçbir HTTP / network / dosya IO doğrudan yapılmaz.

## Invariants & gotchas
- **Thread-safe DEĞİL:** Backing store sade `HashMap`; eşzamanlı kullanım için dış senkronizasyon veya `ConcurrentHashMap` gerekir.
- **Persistence yok:** Süreç restart olunca tüm müşteriler kaybolur. Bu sınıf prod-ready bir repository değil, in-memory bir prototiptir.
- **E-posta validasyonu naive:** Sadece `email.contains("@")` kontrolü var; gerçek format/MX validasyonu yapılmaz.
- **Duplicate kontrolü yalnızca aktif kayıtlar üzerinde:** `deletedAt != null` olanlar hesaba katılmaz, yani soft-deleted bir müşterinin e-postası yeniden kullanılabilir.
- **`update` tam değişim mantığında:** Kısmi patch değil; verilen `fullName` ve `email` direkt set edilir. `null` geçmek değer silmek anlamına gelir.
- **E-posta değişirse `emailVerifiedAt` sıfırlanır** — kullanıcı yeniden doğrulama yapmak zorundadır.
- **`verifyEmail` idempotenttir:** Zaten doğrulanmış müşteri için tokenı tüketmeden mevcut kaydı döner.
- **`softDelete` sonrası `findById` `Optional.empty()` döner**, ancak kayıt store'da fiziksel olarak kalmaya devam eder; hard delete mekanizması yoktur.
- **`deactivate` `softDelete`'in süper-kümesi:** Aynı şekilde `deletedAt` damgalar, ek olarak `emailVerifiedAt`'i sıfırlar. Bu yüzden olası bir reaktivasyon (örn. `deletedAt`'i tekrar `null`'a çekecek bir akış) sonrasında müşteri yeniden e-posta doğrulamak zorunda kalır. `softDelete` ile aynı şekilde idempotent değildir: ikinci çağrı `CustomerNotFoundException` atar (çünkü `requireActive` artık başarısız olur).
- **Zaman soyutlaması yok:** `Instant.now()` doğrudan çağrılır; testlerde sabit zaman üretmek için ya bytecode-level mock ya da bir `Clock` refactor'ü gerekir.
- **Exception'lar `RuntimeException`:** Çağıran tarafa checked exception zorlaması yapılmaz.

## Related docs
- `.cursor-docs/src/main/java/com/example/business/Customer.md` *(henüz oluşturulmadı)*
- `.cursor-docs/src/main/java/com/example/business/AuditLogger.md` *(henüz oluşturulmadı)*
- `.cursor-docs/src/main/java/com/example/business/EmailVerificationQueue.md` *(henüz oluşturulmadı)*

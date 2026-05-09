# CustomerService

> **Source:** `src/services/CustomerService.ts`
> **Last verified:** `2026-05-09`
> **Status:** `fresh`

## Purpose
Müşteri yaşam döngüsü işlemlerini (oluşturma, güncelleme, soft-delete, e-posta
doğrulama tetikleme) tek noktadan yöneten domain servisi. Repository ile dış dünya
arasındaki iş kuralı katmanıdır; HTTP/transport bilgisi taşımaz.

## Public API
- `create(input: CreateCustomerInput) -> Customer` — yeni müşteri oluşturur, e-posta
  doğrulama maili kuyruğa atar.
- `update(id: CustomerId, patch: CustomerPatch) -> Customer` — kısmi güncelleme;
  e-posta değişirse yeniden doğrulama tetiklenir.
- `softDelete(id: CustomerId) -> void` — `deletedAt` damgalar, fiziksel silme yok.
- `findById(id: CustomerId) -> Customer | null` — read-through cache.
- `verifyEmail(token: string) -> Customer` — token'ı tüketir, `emailVerifiedAt` set eder.

## Dependencies
- `CustomerRepository` — kalıcılık (Postgres).
- `EmailVerificationQueue` — outbox pattern ile mail kuyruğa yazılır.
- `Clock` — test edilebilirlik için zaman soyutlaması.
- `Logger` — yapılandırılmış loglama.

## Side effects
- DB write: `customers` tablosu (insert/update/soft-delete).
- DB write: `email_verification_outbox` tablosu (create/verify yollarında).
- Cache invalidation: `customer:{id}` anahtarı update ve softDelete sonrası silinir.
- Hiçbir HTTP / network çağrısı doğrudan yapılmaz; e-postalar worker tarafından gönderilir.

## Invariants & gotchas
- `create` idempotent **değildir**; aynı e-posta ile iki kez çağrılırsa
  `DuplicateEmailError` fırlatır (DB'de unique index var).
- `verifyEmail` idempotenttir; aynı token ikinci kez kullanılırsa son durumu döner,
  hata fırlatmaz.
- `softDelete` sonrası `findById` `null` döner; hard delete sadece batch job ile
  90 gün sonra yapılır.
- Tüm metodlar `CustomerId` branded type ister; ham string geçilemez.
- `Clock` enjekte edilmediği için testlerde `FakeClock` kullanılmalı.

## Related docs
- `.cursor-docs/src/repositories/CustomerRepository.md`
- `.cursor-docs/src/queues/EmailVerificationQueue.md`
- `.cursor-docs/src/domain/Customer.md`

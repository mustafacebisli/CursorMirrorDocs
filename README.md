# Find-or-Create Documentation System for Cursor

Bu repo, **kodu kirletmeden** ve **token maliyetini düşürerek** Cursor'a class/dosya
bağlamını verebilmek için bir iskelet içerir. Mantık basit:

> Bir class'ı sorduğunda Cursor önce `.cursor-docs/` altında o class'ın özetine bakar.
> Varsa onu okur (ucuz). Yoksa kaynak kodu bir kez okur, özet üretir, `.cursor-docs/`
> içine yazar ve sonraki tüm sorularda o özetten cevap verir.

## Dosya yapısı

```
project-root/
├── .cursor/
│   └── rules/
│       └── find-or-create-docs.mdc   # Cursor'a verilen davranış kuralı
├── .cursor-docs/
│   ├── README.md                     # bu sistemin nasıl çalıştığı
│   ├── _TEMPLATE.md                  # yeni dökümanlar için şablon
│   └── _examples/
│       └── CustomerService.md        # örnek doldurulmuş döküman
├── src/                              # GERÇEK kodun (sen ekleyeceksin)
└── README.md                         # bu dosya
```

## Kurulum (kendi projene taşımak için)

Aşağıdaki dosya/klasörleri kendi projenin köküne kopyala:

- `.cursor/rules/find-or-create-docs.mdc`
- `.cursor-docs/README.md`
- `.cursor-docs/_TEMPLATE.md`
- `.cursor-docs/_examples/` (opsiyonel, referans için)

`src/` zaten kendi kodun. Başka bir şey yapmana gerek yok — Cursor `.cursor/rules/`
altındaki kuralı projeyi açtığında otomatik yükler.

## Kullanım

### "İncele" diyerek doküman üret/oku

```
Sen: CustomerService class'ını incele
```

İlk seferde Cursor:
1. `.cursor-docs/src/services/CustomerService.md` var mı diye bakar.
2. Yoksa, gerçek kaynak dosyayı (`src/services/CustomerService.ts`) okur.
3. Özet üretir ve `.cursor-docs/src/services/CustomerService.md`'e yazar.
4. Sana cevap verir.

İkinci seferde aynı soruyu sorduğunda Cursor sadece markdown dosyasını okur — kaynak
kodu **hiç açmaz**. Token maliyeti dramatik şekilde düşer.

### Dökümanı zorla yenile

Kod değiştiyse veya özetin güncel olmadığını düşünüyorsan:

```
Sen: CustomerService dökümanını yenile
```

veya o dökümanın header'ında `Status: stale` yaz; bir sonraki "incele" çağrısında
otomatik regenerate edilir.

### Kod değiştirme + döküman güncelleme

Kural, kaynak kodu **değiştirdiğin** her durumda ilgili dökümanın da aynı turda
güncellenmesini söylüyor. Yani:

```
Sen: CustomerService'e softDeleteAll metodu ekle
```

Cursor: kodu okur, metodu ekler, **sonra** `.cursor-docs/.../CustomerService.md`'i de
yeni public API'yi yansıtacak şekilde günceller.

## Önemli notlar (gerçekçi beklenti seti)

1. **Cursor rules öneridir, garanti değildir.** Model zaman zaman gerek görürse kaynak
   kodu yine açabilir. Token tasarrufu istatistiksel olarak gerçek, ama %100 deterministik
   değil. Pratikte iyi çalışır.

2. **Modifikasyon işlemlerinde tasarruf yok.** "X metodunu refactor et" tarzı bir istek
   geldiğinde model gerçek kodu okumak **zorunda**. Bu sistem sadece "bu class ne yapıyor"
   tarzı keşif sorularında ucuzluk sağlar.

3. **Eskime gerçek bir risk.** Kod ile döküman ayrı yerlerde yaşadığı için sapabilirler.
   Kuralın Step 5'i bu sapmayı azaltır ama elimine etmez. Düzenli olarak `Status: stale`
   ile işaretle veya "tüm dökümanları yenile" çağrısı yap.

4. **Kazanç ne zaman büyür?** 50+ class'lı bir projede aynı class'ı haftalarca defalarca
   sorduğunda. Küçük projede fark hissedilmez.

## Sık sorulan

**Q: Kural otomatik mi devreye giriyor?**
A: Evet. `.cursor/rules/*.mdc` dosyaları Cursor tarafından otomatik yüklenir. Bu kuralın
header'ında `alwaysApply: true` set edildiği için her sohbette aktif.

**Q: Dökümanları git'e commit etmeli miyim?**
A: Evet. Takım arkadaşların ve gelecekteki Cursor oturumların aynı önbellekten
yararlanır. `.cursor-docs/` versiyonlanmalı.

**Q: Kaynak değiştiğinde dökümanı otomatik stale yapan bir şey var mı?**
A: Hayır, native bir şey yok. Pre-commit hook ile yapılabilir (kaynak değişti +
dökümanın `Last verified` tarihi eski → otomatik `Status: stale` damgalama). İstersen
bu hook'u da ekleyebiliriz.

## Ekip kullanımı

Birden fazla geliştirici aynı projede çalışıyorsa, dökümanların eskimemesi için **iki
katmanlı koruma** var:

### Katman 1 — Yumuşak: AI'ya talimat
`.cursor/rules/sync-docs-on-change.mdc` her sohbette aktif. AI bir kaynak dosyayı
değiştirirse, mirror'daki dökümanı **aynı diff içinde** günceller. Public API
değişti ise ilgili bölümleri yeniler; sadece içsel refactor ise yalnızca
`Last verified` tarihini bumplar. Bu kural Cursor üzerinden yapılan değişiklikler
için yeterli.

### Katman 2 — Sert: pre-commit hook
`.githooks/pre-commit` script'i, **manuel** (Cursor dışı) yapılan değişiklikleri
yakalar. Bir geliştirici `src/` altındaki bir dosyayı değiştirir ama mirror
dökümanı güncellemezse, hook o dökümanın header'ındaki `Status: fresh` değerini
`Status: stale` yapar ve commit'e dahil eder. Commit engellenmez; sadece bir
sonraki "incele" çağrısında otomatik olarak yenilenmesi tetiklenir.

### Hook'u açma (her geliştirici bir kez yapar)

```bash
git config core.hooksPath .githooks
```

Bu komutu projeyi yeni clone'layan herkes çalıştırmalı. Bunu unutulmaz hale
getirmek için projeye bir `npm run setup` veya `make setup` komutu ekleyip
orada bu satırı çalıştırabilirsin.

### Ekip akışı özeti

| Senaryo | Olan |
|---|---|
| Cursor üzerinden kod değiştirme | AI doc'u aynı turda günceller (Katman 1) |
| Manuel kod değiştirme + commit | Hook doc'u stale işaretler (Katman 2) |
| Stale doc'lu sınıfı sorma | AI otomatik yeniler ve cevap verir |
| İki kişi aynı doc'u değiştirir | Normal git merge conflict; markdown küçük olduğu için kolay çözülür |

### Acil durumda hook'u atlama

```bash
git commit --no-verify -m "..."
```

Sadece gerçekten gerektiğinde kullan, yoksa drift birikir.

## Sonraki adımlar (opsiyonel)

- [x] `Status: stale` damgalayan pre-commit hook (**eklendi:** `.githooks/pre-commit`)
- [ ] CI tarafında `Status: stale` olan dökümanları raporlayan bir job
- [ ] Dependency graph'i otomatik üreten script (`madge`, `jdeps`, `pydeps` gibi
      araçlarla)
- [ ] `make docs:audit` benzeri komut ile tüm `Last verified` tarihlerini raporla

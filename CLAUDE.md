# CLAUDE.md

Ten plik zawiera wskazówki dla Claude Code podczas pracy z tym repozytorium.

## Cel

To jest biblioteka autokonfiguracji Spring Boot używana przez główną aplikację (`PoorBetApplication`).

Nie jest to samodzielny serwis — nie posiada klasy `main`, nie uruchamia własnego serwera (brak embedded serwera).

## Jak testować lokalnie
mvn install
Następnie w (`PoorBetApplication`) zależność jest automatycznie pobierana z lokalnego repozytorium Maven (~/.m2)

## Język komunikacji

Zawsze odpowiadaj w języku polskim.

Cała dokumentacja, komentarze do kodu, opisy zmian, propozycje architektoniczne, komunikaty commitów oraz przykłady kodu powinny być przygotowywane w języku polskim, chyba że użytkownik wyraźnie poprosi o użycie innego języka.

## Przegląd projektu

To repozytorium zawiera **bibliotekę autokonfiguracji Spring Boot** (nie jest samodzielną aplikacją uruchamialną), publikowaną do GitHub Packages.

Biblioteka dostarcza współdzieloną infrastrukturę uwierzytelniania i autoryzacji wykorzystywaną przez mikroserwisy systemu PoorBet.

Plugin `spring-boot-maven-plugin` jest celowo wyłączony (`<skip>true</skip>`), ponieważ projekt jest biblioteką, a nie aplikacją wykonywalną.

### Publikacja

Biblioteka publikowana jest do:

```text
https://maven.pkg.github.com/Dariusz95/PoorBetAuthStarter
```

## Powiązane repozytoria

Projekt PoorBet składa się z kilku repozytoriów:

* poorbet-app – główna aplikacja mikroserwisowa
* poorbet-auth-starter – współdzielona autokonfiguracja Spring Security (to repozytorium)
* poorbet-commons – biblioteka współdzielonych eventów, DTO i komponentów infrastrukturalnych

Zmiany w tym repozytorium mogą wymagać aktualizacji pozostałych repozytoriów.

## Komendy budowania i publikacji

```bash
# Kompilacja oraz uruchomienie testów
./mvnw verify

# Budowanie biblioteki bez uruchamiania testów
./mvnw package -DskipTests

# Publikacja do GitHub Packages
# Wymaga ustawionej zmiennej środowiskowej GITHUB_TOKEN
./mvnw --batch-mode deploy
```

Aktualnie repozytorium nie zawiera testów.

Workflow CI/CD `publish-poorbet-auth-starter.yml` publikuje nową wersję biblioteki przy każdym pushu do gałęzi `main`.

## Architektura

W pliku:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

zarejestrowane są dwie niezależne autokonfiguracje.

---

## 1. PoorbetSecurityAutoConfiguration

Prefiks konfiguracji:

```yaml
poorbet.security.*
```

Autokonfiguracja aktywowana jest, gdy:

```yaml
poorbet.security.enabled=true
```

(wartość domyślna)

Konfiguruje OAuth2 Resource Server oparty o JWT oraz dwa niezależne łańcuchy bezpieczeństwa (`SecurityFilterChain`).

### internalSecurityFilterChain

Najwyższy priorytet.

Obsługuje ścieżki:

```text
/internal/**
```

Wymagania:

* token typu `service`
* weryfikacja audience względem `poorbet.security.internal-audience`

### apiSecurityFilterChain

Drugi priorytet.

Obsługuje ścieżki:

```text
/api/**
/actuator/**
```

Wymagania:

* token typu `user`

### PermissionClaimConverter

Konwerter odpowiedzialny za mapowanie uprawnień JWT na Spring Security `GrantedAuthority`.

Odczytuje dane z pól:

* `permissions`
* `scope`
* `roles`

Role automatycznie otrzymują prefiks:

```text
ROLE_
```

### Security metod

Biblioteka aktywuje:

```java
@EnableMethodSecurity
```

Dzięki temu mikroserwisy mogą wykorzystywać:

```java
@PreAuthorize(...)
```

oraz stałe z klasy:

```java
PoorbetPermissions
```

### Typy tokenów

Rozróżnienie tokenów odbywa się poprzez claim:

```text
token_type
```

Obsługiwane wartości:

```text
user
service
```

Stałe znajdują się w klasie:

```java
PoorbetTokenTypes
```

### Wymagane właściwości

```yaml
poorbet:
  security:
    jwk-set-uri: <adres endpointu JWKS>
    issuer: poorbet-auth-service
    internal-audience: []
    unprotected-paths: []
```

Opis:

* `jwk-set-uri` – adres endpointu JWKS
* `issuer` – oczekiwany issuer tokena
* `internal-audience` – lista dozwolonych audience dla endpointów `/internal/**`
* `unprotected-paths` – ścieżki pomijające uwierzytelnianie

---

## 2. AuthAutoConfiguration

Prefiks konfiguracji:

```yaml
auth.service.*
```

Autokonfiguracja aktywowana jest po ustawieniu:

```yaml
auth.service.url
```

Dostarcza infrastrukturę do komunikacji między mikroserwisami.

### ServiceTokenProvider

Odpowiada za:

* pobieranie tokenu typu Client Credentials
* cache tokenu
* automatyczne odświeżanie tokenu 30 sekund przed wygaśnięciem

Token pobierany jest z endpointu:

```http
POST /oauth/token
```

Implementacja wykorzystuje:

* volatile fields
* synchronized
* double-checking

w celu zapewnienia bezpieczeństwa wielowątkowego.

### JwtForwardingInterceptor

Interceptor dla `RestClient`.

Przekazuje JWT aktualnego użytkownika z:

```java
SecurityContextHolder
```

do kolejnych wywołań HTTP.

### ServiceJwtForwardingInterceptor

Interceptor dla `RestClient`.

Dodaje token serwisowy pobrany z `ServiceTokenProvider`.

Nagłówek nie zostanie dodany, jeżeli żądanie zawiera:

```http
X-Skip-Auth
```

### authRestClient

Dedykowany bean typu:

```java
RestClient
```

skonfigurowany z adresem:

```yaml
auth.service.url
```

Wykorzystywany przez `ServiceTokenProvider`.

### Wymagane właściwości

```yaml
auth:
  service:
    url: <adres auth-service>
    client-id: <identyfikator klienta>
    client-secret: <sekret klienta>

    timeout:
      connect: 5s
      read: 10s
```

## Kluczowe wzorce projektowe

### Możliwość nadpisywania beanów

Wszystkie komponenty wykorzystują:

```java
@ConditionalOnMissingBean
```

Dzięki temu aplikacje korzystające z biblioteki mogą nadpisywać dowolne implementacje.

### Dedykowany authRestClient

Bean `authRestClient` posiada własną nazwę:

```java
"authRestClient"
```

Pozwala to uniknąć konfliktów z innymi beanami typu `RestClient`.

### Ręczna konfiguracja interceptorów

`ServiceJwtForwardingInterceptor` nie jest automatycznie dodawany do wszystkich klientów HTTP.

Musi zostać jawnie skonfigurowany w mikroserwisie korzystającym z biblioteki.

## Cel biblioteki

Biblioteka powinna pozostać lekka i skupiona wyłącznie na infrastrukturze bezpieczeństwa.

Nie należy dodawać do niej:

* logiki biznesowej
* endpointów REST
* encji domenowych
* zależności od konkretnych mikroserwisów

Biblioteka powinna dostarczać wyłącznie współdzielone mechanizmy uwierzytelniania, autoryzacji oraz komunikacji między serwisami.

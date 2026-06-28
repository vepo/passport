# Architecture & Conventions

Canonical reference for developers and AI agents working on Passport. Domain terms: [docs/domain-specification.md](docs/domain-specification.md).

## 1. Core principles

- **Central identity service** — Users, profiles, roles, JWT auth, password lifecycle.
- **REST/JSON** — All APIs under `/api/` (Quarkus REST); Backoffice SPA consumes via proxy.
- **Profile-based authorization** — Users hold **Profiles**; profiles grant **Roles**; JWT `groups` = flattened role names.
- **Schema migrations** — Flyway in `src/main/resources/db/migration/`.
- **SPA hosting** — `SPARouting` reroutes unknown paths to `/` for embedded or proxied frontends.

## 2. Request lifecycle

1. Client sends JSON to `@Path` endpoint (`@ApplicationScoped` or plain JAX-RS bean).
2. `@RolesAllowed` gates admin APIs (`passport.admin`).
3. Public auth routes (`/auth/login`, password reset) have no role gate.
4. Endpoint uses `*Repository` (or `PasswordEncoder`, `JwtGenerator`, `MailerService`).
5. JSON response records (`*Response`) or HTTP status.

## 3. Domain model

| Entity | Table | Role |
|--------|-------|------|
| `User` | `tb_users` | Account: username, name, email, encoded password, disabled flag |
| `Profile` | `tb_profiles` | Named bundle of roles assigned to users |
| `Role` | `tb_roles` | Permission string (e.g. `passport.admin`, `domains.admin`) |
| `ResetPasswordToken` | `tb_reset_password_tokens` | Hashed token for password reset flow |
| `Notification` | `tb_notifications` | Cross-service event payload (source, title, report JSON) |
| `NotificationItem` | `tb_notification_items` | Sub-task / API call report attached to a notification |
| `UserNotification` | `tb_user_notifications` | Per-user delivery and read state |
| `ChannelFollow` | `tb_channel_follows` | User subscription to an Engage channel id |

Relations:
- `User` ↔ `Profile` — `tb_users_profiles` (many-to-many)
- `Profile` ↔ `Role` — `tb_profile_roles` (many-to-many)

JWT `groups` claim = distinct role names from all active profiles of the user (`JwtGenerator`).

## 4. Auth API

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `POST` | `/auth/login` | Public | Email + password → JWT |
| `GET` | `/auth/me` | JWT | Current user info |
| `POST` | `/auth/change-password` | JWT | Change password |
| `POST` | `/auth/request-reset-password` | Public | Email reset link |
| `POST` | `/auth/reset` | Public | Confirm reset with token |

Public auth routes are rate-limited per client IP (`AuthRateLimitFilter`): default 30 requests/minute on login and password reset paths. Configure via `passport.auth.rate-limit.*`.

## 5. User API

Admin (`passport.admin`) unless noted.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/users` | Create user |
| `GET` | `/users/{userId}` | Find by id |
| `PUT` | `/users/{userId}` | Update user |
| `GET` | `/users/search` | Search users |
| `POST` | `/users/{userId}/profiles` | Assign profiles |
| `POST` | `/users/{userId}/enable` | Enable user |
| `POST` | `/users/{userId}/disable` | Disable user |

## 6. Profile API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/profiles` | Create profile |
| `GET` | `/profiles` | List profiles |
| `GET` | `/profiles/{profileId}` | Find by id |
| `PUT` | `/profiles/{profileId}` | Update profile |
| `GET` | `/profiles/search` | Search profiles |
| `POST` | `/profiles/{profileId}/roles` | Assign roles |
| `POST` | `/profiles/{profileId}/enable` | Enable profile |
| `POST` | `/profiles/{profileId}/disable` | Disable profile |

## 7. Role API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/roles` | Create role |
| `GET` | `/roles` | List roles |
| `GET` | `/roles/search` | Search roles |
| `DELETE` | `/roles/{roleId}` | Delete role |

## 7.1 Notification API

User APIs (`@Authenticated` — any logged-in user):

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/notifications` | List current user's notifications (`?unread=true` optional) |
| `GET` | `/notifications/unread-count` | Unread count for shell badge |
| `GET` | `/notifications/by-channel/{engageChannelId}` | All sync reports for channel (`engage.admin`) |
| `GET` | `/notifications/{id}` | Detail + items; marks opened/read if not yet read |
| `PATCH` | `/notifications/{id}/read` | Mark read |
| `PATCH` | `/notifications/{id}/unread` | Mark unread |

Channel follows (Engage channel id, no cross-DB FK):

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/channel-follows` | List followed Engage channel ids |
| `POST` | `/channel-follows` | Follow `{ engageChannelId }` (idempotent) |
| `DELETE` | `/channel-follows/{engageChannelId}` | Unfollow |
| `GET` | `/channel-follows/{engageChannelId}/status` | `{ following: boolean }` |

Internal API (service key, no user JWT):

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `POST` | `/internal/notifications` | Header `X-Service-Key` | Create notification + fan-out to channel followers |

Filter: `InternalServiceKeyFilter` on paths under `internal/`. Config: `passport.internal.service-key`.

## 8. Design patterns

### Repository

- `UserRepository`, `ProfileRepository`, `RoleRepository` — `EntityManager`, `@Transactional` on writes.

### Services

- `PasswordEncoder`, `JwtGenerator`, `MailerService` — cross-cutting auth/mail.
- CDI events: `UserCreatedEvent`, `ResetPasswordCreatedEvent` → email observers.

### Testing

- `@QuarkusTest` + RestAssured; `Given` builders in `dev.vepo.passport.shared`.

## 9. Package layout

```
dev.vepo.passport/
├── auth/             # Login, JWT, password change/reset, current user
├── mailer/           # Transactional email + CDI events
├── model/            # User, Profile, Role, ResetPasswordToken entities
├── profile/          # Profile CRUD, assign roles, enable/disable
├── role/             # Role CRUD, search, delete
├── notification/     # NotificationService, user + internal endpoints
├── channelfollow/    # Channel follow CRUD
├── user/             # User CRUD, assign profiles, enable/disable
└── shared/
    ├── exception/    # Exception mappers, ErrorResponse
    ├── infra/        # DatabaseDevSetup
    ├── routing/      # SPARouting
    ├── security/     # PasswordEncoder, RequiredRoles, InternalServiceKeyFilter, AuthRateLimitFilter
    └── templating/   # Qute extensions (if used)
```

Feature endpoints use verb subpackages: `user/create/CreateUserEndpoint`, etc.

## 10. Naming

| Kind | Pattern |
|------|---------|
| Endpoint | `XxxEndpoint` |
| Repository | `XxxRepository` |
| Entity | singular PascalCase in `model/` |
| Request/response | `XxxRequest`, `XxxResponse` records |
| Test | `XxxEndpointTest` with `@QuarkusTest` |

## 11. Authentication & roles

| Role name | Meaning |
|-----------|---------|
| `passport.admin` | Manage users, profiles, roles (`RequiredRoles.ADMIN`) |
| `domains.admin` | Granted via profile; used by Visita domain admin API |
| `Domain.Editor` | Visita domain editing (via profile) |
| `Domain.Stats.Viewer` | Visita stats viewing (via profile) |

JWT issuer: `mp.jwt.verify.issuer` (default `https://passport.vepo.dev`).

## 12. Database (main tables)

- `tb_users`, `tb_profiles`, `tb_roles`
- `tb_users_profiles`, `tb_profile_roles`
- `tb_reset_password_tokens`
- `tb_notifications`, `tb_notification_items`, `tb_user_notifications`, `tb_channel_follows`

DDL: `src/main/resources/db/migration/`

## 13. Adding a feature (checklist)

1. Flyway migration if schema changes.
2. Entity (in `model/`) + repository.
3. Endpoint + request/response records.
4. Update `dev-import.sql` for dev personas ([development-experience.mdc](.cursor/rules/development-experience.mdc)).
5. `@QuarkusTest` + `Given` updates.
6. Update [docs/domain-specification.md](docs/domain-specification.md) if domain language changes.
7. **Update this file** — routes, tables, packages.

## 14. Development setup

```bash
./mvnw quarkus:dev
```

- Default HTTP port **8080**.
- `%dev.quarkus.flyway.clean-at-start=true`; `DatabaseDevSetup` runs `dev-import.sql`.
- Dev users password: same hash as `password.default` in `application.properties` (see dev-import comment: `qwas1234`).
- Mailer: Mailtrap in `%dev` for reset emails.
- `base.url` points to Backoffice (`http://localhost:4200`) for reset links.

## 15. Configuration (selected)

```properties
quarkus.flyway.migrate-at-start=true
%dev.quarkus.flyway.clean-at-start=true
mp.jwt.verify.issuer=${JWT_ISSUER:https://passport.vepo.dev}
password.algorithm=PBKDF2WithHmacSHA512
base.url=https://backoffice.vepo.dev
passport.internal.service-key=${PASSPORT_INTERNAL_SERVICE_KEY:dev-internal-service-key}
passport.auth.rate-limit.enabled=true
passport.auth.rate-limit.max-requests=30
passport.auth.rate-limit.window=PT1M
```

## 16. Common pitfalls

- **Disabled users/profiles** — login and JWT role resolution must exclude disabled entities (`findActiveByEmail`).
- **Role names in JWT** — flattened from profiles; changing profile-role assignment requires re-login.
- **Username length** — max 15 chars (`User.username`).
- **Delete role** — ensure no profile still references it.

## 17. CI

`.github/workflows/maven.yml`: `mvn clean compile`, `mvn test`. Native Docker image on push to `main` / tags.

## 18. Consumers

- **Backoffice** — Angular admin UI; proxy `/passport/api/**` → Passport.
- **Visita** — Validates JWT role `domains.admin` for domain admin API.
- **Engage** — Publishes sync run reports via `POST /api/internal/notifications` (service key).

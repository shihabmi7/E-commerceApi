# Spring Data JPA — Fetch Types, N+1, and `open-in-view`

Grounded in a real fix made in this repo: `Cart.user` / `Cart.product` (see `src/main/java/com/shihab/ecommerceapi/model/Cart.java` and `CartRepository.java`), PR #9.

## 1. What's the default fetch type for `@ManyToOne`? Is it the same for `@OneToMany`?
No — this asymmetry is a classic JPA gotcha.

| Annotation    | Default fetch |
|---------------|---------------|
| `@ManyToOne`  | EAGER         |
| `@OneToOne`   | EAGER         |
| `@OneToMany`  | LAZY          |
| `@ManyToMany` | LAZY          |

`EAGER` means: load the related entity in the same breath as the parent, whether the caller asked for it or not. `LAZY` means: don't load it until someone actually calls the getter — at which point Hibernate fires an extra query, *if* a session is still open.

Before the fix, `Cart.java` had plain `@ManyToOne private Product product;` — silently EAGER, with no annotation making that visible.

## 2. What's actually wrong with leaving `@ManyToOne` on its EAGER default?
Every load of the parent entity unconditionally pulls in the associated row, even in code paths that never touch it (e.g. just counting `Cart` rows). You pay the join cost every time, with no way to opt out per-query — Hibernate decided at mapping time, not at the point where you actually know whether you need the data.

## 3. What's the N+1 problem, concretely, for `Cart` → `Product`?
If `Cart.product` were LAZY with no explicit fetch, and code looped over a user's cart accessing `cart.getProduct()` for each line:

```java
List<Cart> carts = cartRepository.findByUserId(userId); // 1 query
carts.forEach(c -> c.getProduct().getName());            // N more queries, one per cart row
```

1 query for the carts + N queries (one per product) = N+1 total, instead of a single query with a join.

## 4. How do you fix N+1 without going back to blanket EAGER?
Make the association `LAZY` (explicit, visible, controllable), then force an eager fetch **only on the specific queries that need it**, using `@EntityGraph` or JPQL `JOIN FETCH`.

```java
// Cart.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id")
private Product product;
```

```java
// CartRepository.java
@EntityGraph(attributePaths = {"user", "product"})
@Override
List<Cart> findAll();

@EntityGraph(attributePaths = {"user", "product"})
@Override
Optional<Cart> findById(Integer id);

@EntityGraph(attributePaths = {"user", "product"})
List<Cart> findByUserId(Integer userId);

@EntityGraph(attributePaths = {"user", "product"})
Optional<Cart> findByUserIdAndProductId(Integer userId, Integer productId);
```

`@EntityGraph` tells Hibernate to fetch `user`/`product` in the *same* SQL query as `Cart` (one JOIN), only for the methods that declare it — not for every load of a `Cart` everywhere in the app.

## 5. Do you need `@EntityGraph` every time you use `LAZY`?
No. `LAZY` alone is a complete, valid setup — it just means "don't load until asked." You only need `@EntityGraph`/`JOIN FETCH` when **both** are true:
1. You know the caller will access the lazy field, **and**
2. That access happens after the Hibernate session/transaction has already closed.

## 6. What is `spring.jpa.open-in-view`, and why does it matter here?
It controls how long the Hibernate session stays open during a web request.

- **`true`** (Spring Boot's actual factory default): the session stays open through the whole request, including the controller and JSON serialization. Lazy fields "just work" wherever you touch them — but that hides N+1 queries instead of preventing them, and it holds a DB connection for the entire request (risky for connection-pool exhaustion under load). Considered an anti-pattern by most teams for that reason.
- **`false`** (what this project uses — see `application.properties` and `application-test.properties`): the session closes as soon as the `@Transactional` repository/service method returns, *before* the controller builds its response. Safer for connection pooling, but touching a lazy field afterward throws `LazyInitializationException` immediately instead of silently querying again.

Because this repo has `open-in-view=false`, simply marking `Cart.product` as `LAZY` without `@EntityGraph` would have broken `CartController.getCart` and `addToCart` — both read `cart.getProduct()`/`getUser()` after the repository call returns, once the session is already closed. The `@EntityGraph` forces that data to be loaded *while the session is still open*, so it's safe to read afterward.

## 7. Why not just add `@Transactional` to the controller/service and keep it EAGER?
`@Transactional` on `CartService.addToCart` specifically can't be used here for an unrelated reason: it wraps the optimistic-insert-then-catch-`DataIntegrityViolationException` retry logic used to fix a separate race condition (two concurrent "add to cart" requests for the same user+product) — see the comment in `CartService.java`. Postgres poisons the whole transaction on any SQL error, which would break the fallback query in the `catch` block. Keeping the fetch decision at the query level (`@EntityGraph`), rather than the transaction-boundary level, avoids that conflict entirely.

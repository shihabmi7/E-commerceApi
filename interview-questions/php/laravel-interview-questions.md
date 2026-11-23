# Laravel — Top 10 Interview Questions (Easy Answers)

## 1. What is Laravel, and why use it?

Laravel is a PHP framework that uses the MVC pattern (Model, View, Controller). It gives you routing, database tools, login, queues, and caching out of the box, so you write less code and build faster.

If you know Spring Boot, think of it as the PHP version of the same idea.

## 2. What is the service container?

It is Laravel's DI container (like Spring's bean container). Laravel creates objects and injects them into your classes.

```php
class OrderController extends Controller
{
    public function __construct(private PaymentService $payment) {}
}
```

Laravel sees the type `PaymentService` and injects it. You can bind an interface to a class in a service provider:

```php
$this->app->bind(PaymentInterface::class, StripePayment::class);
```

## 3. What is Eloquent ORM?

Eloquent maps each database table to a PHP model class. You work with objects instead of raw SQL.

```php
$user = User::find(1);
$orders = Order::where('status', 'paid')->get();
```

It uses the **Active Record** pattern: the model itself can save, update, and delete.

## 4. What are the relationship types in Eloquent?

| Type | Example |
|---|---|
| `hasOne` | A user has one profile |
| `hasMany` | An order has many items |
| `belongsTo` | An item belongs to an order |
| `belongsToMany` | Users and roles (uses a pivot table) |
| `hasManyThrough` | A country has many posts through users |

## 5. What is the N+1 problem in Laravel? How do you fix it?

Same as in Hibernate: one query for the list, then one extra query per item.

```php
// Bad: N+1
$orders = Order::all();
foreach ($orders as $o) { echo $o->items->count(); }

// Good: eager loading
$orders = Order::with('items')->get();
```

Use `with()` to eager load. In development, `Model::preventLazyLoading()` catches these problems early.

## 6. What is middleware?

Middleware is code that runs before or after a request reaches your controller. Use it for login checks, logging, rate limits, and CORS. It is similar to filters or interceptors in Spring.

```php
Route::get('/orders', [OrderController::class, 'index'])
     ->middleware('auth');
```

## 7. What are migrations and seeders?

- **Migrations** are version control for your database. You write table changes in PHP, and everyone on the team runs the same changes.
- **Seeders** fill the database with test or starting data.

```bash
php artisan make:migration create_orders_table
php artisan migrate
php artisan db:seed
```

## 8. How do queues and jobs work?

Queues run slow tasks in the background (email, reports) so the user does not wait.

1. Your code dispatches a job: `SendInvoiceEmail::dispatch($order);`
2. The job goes to a queue (Redis, database, or SQS).
3. A worker picks it up: `php artisan queue:work`.

Failed jobs can retry automatically. You can set the number of tries and the delay.

## 9. How do you validate requests and keep the app secure?

**Validation:** use a Form Request class.

```php
public function rules(): array
{
    return [
        'email' => 'required|email|unique:users',
        'amount' => 'required|numeric|min:1',
    ];
}
```

**Security built into Laravel:**

- **SQL injection:** Eloquent and the query builder use prepared statements.
- **XSS:** Blade's `{{ }}` escapes output automatically.
- **CSRF:** a token is checked on every form (`@csrf`).
- **Passwords:** hashed with `Hash::make()`.
- **Mass assignment:** protect it with `$fillable` in the model.

## 10. Route types, API Resources, and Sanctum

- `Route::get`: one route for one URL.
- `Route::resource('orders', OrderController::class)`: all CRUD routes in one line.
- `Route::apiResource(...)`: same, without the HTML form routes (create, edit). Use this for APIs.
- **API Resources (`JsonResource`)**: control how a model turns into JSON.
- **Sanctum**: simple token login for mobile apps and single-page apps.

## Bonus questions

- **What is `artisan`?** The Laravel command-line tool.
- **`find()` vs `first()` vs `get()`?** One by ID, one record, many records.
- **What are Facades?** A short static-style way to reach container services, like `Cache::get()`.
- **What are Service Providers?** The place where you register and set up services when the app starts.

## Tip for the interview

Compare with Spring Boot when you answer. Example: "The service container is like Spring's IoC container, and middleware is like a filter."

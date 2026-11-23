# How to Set Up Prometheus and Grafana

A simple guide. Follow the steps in order. It takes about 5 minutes.

![Grafana dashboard](images/grafana-dashboard.png)

## What are these tools?

| Tool | What it does | Address |
|------|--------------|---------|
| **Spring Boot app** | Your API. It shares numbers about itself (requests, orders, memory...) | http://localhost:8080/actuator/prometheus |
| **Prometheus** | Collects those numbers every 5 seconds and stores them | http://localhost:9090 |
| **Grafana** | Draws the numbers as nice graphs | http://localhost:3000 |

```
Your app  -->  Prometheus  -->  Grafana (you look here)
```

You do **not** install anything by hand. Docker runs it all with **one command**:

| Container | What it is |
|-----------|------------|
| `springboot-app` | Your API |
| `postgres` | The database |
| `rabbitmq` | The message queue |
| `prometheus` | Collects the numbers |
| `grafana` | Draws the graphs |

## Before you start

You need only:
- **Docker Desktop**, installed and **running**
- The project downloaded on your computer

(You do **not** need Java, Maven or PostgreSQL installed. Docker brings them.)

## Step 1: Create your settings file (one time)

The app needs a secret key for login tokens. In the project folder:

```bash
cp .env.example .env
openssl rand -base64 32
```

The second command prints a random text. Open the new `.env` file and paste it
after `JWT_SECRET_KEY=`, like this:

```
JWT_SECRET_KEY=paste-the-random-text-here
```

`.env` is private and is never uploaded to GitHub.

## Step 2: Start everything

```bash
docker compose up -d --build
```

- The **first time** it takes a few minutes (it builds your app and downloads images).
- Later starts take seconds.
- Check that all 5 are running:

```bash
docker compose ps
```

The API is now at http://localhost:8080.

## Step 3: Check Prometheus can see your app

1. Open http://localhost:9090/targets
2. Find `ecommerce-api`. The state should be green and say **UP**.

If it says **DOWN**, see [Problems and fixes](#problems-and-fixes) below.

## Step 4: Open Grafana

1. Open http://localhost:3000
2. Log in with username `admin` and password `admin`. (Grafana may ask you to
   change the password. You can click **Skip**.)
3. Click **Dashboards** in the left menu, then **E-Commerce API**.

Direct link: http://localhost:3000/d/ecommerce-api/e-commerce-api

The dashboard is already set up. You do not need to create anything.

## Step 5: Make the graphs move

A new app has no traffic, so the graphs look flat. Send some requests.
Copy and paste these into a terminal, one block at a time.

**1. Create a test user and log in** (saves your login token in `TOKEN`):

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Test User","email":"test@example.com","password":"Passw0rd!"}'

TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"Passw0rd!"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

**2. Send 200 requests** (request rate, latency and endpoint graphs move):

```bash
for i in $(seq 1 200); do
  curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/products
done
```

**3. Create a product with low stock** ("Low stock products" goes up):

```bash
curl -X POST localhost:8080/api/v1/products -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Test Product","price":9.99,"stock":2}'
```

**4. Place an order** ("Orders placed" goes up). An order needs a user, an
address and an item in the cart. Use your real ids in place of `1`:

```bash
# address
curl -X POST localhost:8080/api/v1/addresses -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"user":{"id":1},"street":"1 Main St","city":"Dhaka","country":"BD","postalCode":"1200"}'

# add a product to the cart
curl -X POST localhost:8080/api/v1/cart/add -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"userId":1,"productId":1,"quantity":1}'

# place the order
curl -X POST localhost:8080/api/v1/orders/place -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"userId":1,"addressId":1}'
```

Wait a few seconds and look at Grafana again. The numbers change.

## What the panels mean

| Panel | Meaning |
|-------|---------|
| App up | 1 = the app is running, 0 = it is down |
| Orders placed (total) | Orders placed since the app started |
| Request rate | How many requests per second the app gets |
| Error rate (5xx) | Server errors per second. Should be 0 |
| Requests per endpoint | Which URLs are used most |
| p95 latency | How slow the slowest 5% of requests are |
| JVM heap / CPU | Memory and processor use of the app |
| DB connections | Database connections in use |
| Low stock products | Products with stock below 5 |
| Payments | Payments by status: completed, failed, pending |
| Orders per minute | How fast orders are coming in |

## Stop everything

```bash
docker compose stop        # stop, keep your data
docker compose down        # remove the containers, keep your data
docker compose down -v     # remove everything INCLUDING the database data
```

To start again later: `docker compose up -d`. Your Grafana setup and database are kept.

## Run the app from IntelliJ instead (optional)

Want to run the app in IntelliJ but keep the rest in Docker?

1. Start everything **except** the app, and tell Prometheus the app is on your computer:

   ```bash
   PROMETHEUS_CONFIG=prometheus-host.yml docker compose up -d postgres rabbitmq prometheus grafana
   ```

2. In IntelliJ open **Run > Edit Configurations** and add these under **Environment variables**
   (use the same key as in your `.env`):

   ```
   JWT_SECRET_KEY=<your key>;SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/ecommerce;SPRING_DATASOURCE_USERNAME=postgres;SPRING_DATASOURCE_PASSWORD=postgres;SPRING_RABBITMQ_HOST=localhost
   ```

3. Run the app. Do not also start the `springboot-app` container (both use port 8080).

## Problems and fixes

**Prometheus target is DOWN**
- Is the app running? Check http://localhost:8080/actuator/health
- If the app crashed, read why: `docker compose logs springboot-app`
- Running the app in IntelliJ? Start Prometheus with
  `PROMETHEUS_CONFIG=prometheus-host.yml` (see the section above).

**`docker compose` says `JWT_SECRET_KEY` is missing**
- Do Step 1: create `.env` and put a key in it.

**Grafana graphs are empty**
- Set the time range (top right) to **Last 15 minutes**.
- Send some requests (Step 5). No requests means no graph lines.
- Make sure Prometheus is UP (Step 3).

**"Orders placed" shows 0 after I restarted the app**
- This is normal. The counter lives in the app's memory, so it starts from 0
  every time the app restarts.

**The app (in IntelliJ) fails with `Could not resolve placeholder 'JWT_SECRET_KEY'`**
- Add `JWT_SECRET_KEY` to the IntelliJ environment variables (see the IntelliJ section).

**The console shows `UnknownHostException: rabbitmq`** (IntelliJ only)
- Add `SPRING_RABBITMQ_HOST=localhost` to the IntelliJ environment variables.

**Port already in use (8080, 3000 or 9090)**
- Something else is using that port. Stop the other program, or stop the second
  copy of the app you started by mistake. (The Docker database uses port **5433**,
  so it will not clash with a PostgreSQL you already run on 5432.)

## Where are the files?

| File | What it is |
|------|------------|
| `docker-compose.yml` | Starts the app, database, RabbitMQ, Prometheus and Grafana |
| `.env` | Your private settings (you create it from `.env.example`) |
| `monitoring/prometheus.yml` | Tells Prometheus to collect from the app container |
| `monitoring/prometheus-host.yml` | Same, but for an app running on your computer (IntelliJ) |
| `monitoring/grafana/provisioning/` | Connects Grafana to Prometheus automatically |
| `monitoring/grafana/dashboards/ecommerce-api.json` | The dashboard itself |

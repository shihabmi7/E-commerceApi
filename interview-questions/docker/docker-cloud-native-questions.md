# Docker & Cloud-Native Development — Interview Questions

40 questions on containerization and cloud-native practices, grounded in this repo's own `Dockerfile`, `docker-compose.yml`, `.github/workflows/docker-ci-cd.yml`, and `k8s/` manifests — real files, not hypothetical ones.

## 1. What is Docker, and how is a container different from a VM?
Docker packages an application with everything it needs to run (code, runtime, libraries, config) into a single, portable unit called an **image**, run as a **container**. A container shares the host machine's OS kernel, isolating only the process/filesystem/network — a VM instead virtualizes an entire machine, including its own OS kernel, which is why containers start in milliseconds and use a fraction of the memory a VM does.
```
VM:        Host OS → Hypervisor → Guest OS (full copy) → App
Container: Host OS → Container runtime → App (shares host kernel)
```

## 2. What is a Docker image vs. a container?
An **image** is a read-only template — a set of layered filesystem snapshots plus metadata (what command to run, what ports to expose). A **container** is a running (or stopped) *instance* of that image, with its own writable layer on top. The same image can be started as many independent containers at once — this repo's own image, `shihabmi7/ecommerce-api:latest` (`docker-ci-cd.yml`), is one image that could back any number of running containers.

## 3. What is a Dockerfile? Walk through this repo's.
A text file of instructions Docker follows to build an image, one layer per instruction.
```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/ecommerce_api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
This is a **multi-stage** build (Q5): stage 1 compiles the jar with the full Maven+JDK toolchain, stage 2 only copies the finished jar into a much smaller JRE-only image — the build tools never ship in the final image.

## 4. What's the difference between `CMD` and `ENTRYPOINT`?
`ENTRYPOINT` sets the fixed command that always runs when the container starts; `CMD` supplies default *arguments* to it (and can be fully overridden at `docker run` time, unlike `ENTRYPOINT`). This repo uses `ENTRYPOINT ["java", "-jar", "app.jar"]` — a fixed command, since this image only ever does one thing (run the jar). If it also had `CMD ["--server.port=8080"]`, someone could override just the argument (`docker run image --server.port=9090`) without needing to override the whole entrypoint.

## 5. What is a multi-stage build, and why does this repo use one?
A Dockerfile with more than one `FROM`, where a later stage can selectively `COPY --from=<earlier stage>` only the artifacts it needs — the earlier stage's tools/intermediate files never make it into the final image. This repo's stage 1 (`maven:3.9.9-eclipse-temurin-17`, several hundred MB with the full JDK + Maven) only exists to produce `app.jar`; stage 2 starts fresh from a slim `eclipse-temurin:17-jre-jammy` and copies in just that one file. The final image ships without Maven, the JDK compiler, or any source code — smaller, and a smaller attack surface.

## 6. What are Docker image layers, and how does layer caching work?
Each instruction in a Dockerfile (`FROM`, `COPY`, `RUN`, ...) produces a new, cached filesystem layer stacked on the previous one. On a rebuild, Docker reuses a cached layer **unchanged** as long as that instruction and everything before it are identical to the last build — the moment one instruction's input changes, that layer and every layer after it must be rebuilt from scratch.
```
FROM maven:...          [layer 1 — reused unless the base image changes]
WORKDIR /app             [layer 2]
COPY pom.xml .            [layer 3 — reused unless pom.xml changed]
RUN mvn dependency:go-offline  [layer 4 — reused unless layer 3 changed]
COPY src ./src            [layer 5 — invalidated on every code change]
RUN mvn clean package     [layer 6 — always reruns after layer 5 changes]
```

## 7. Why does this repo `COPY pom.xml` before `COPY src`?
This is a real fix made in this repo's git history (`5eb7ea8 Split Dockerfile COPY to cache Maven dependencies separately`). Without the split, a single `COPY . .` means **any** source file change invalidates that layer — and every layer after it, including `mvn dependency:go-offline`, which re-downloads the entire Maven dependency tree from the internet on every single build. By copying only `pom.xml` first and running `dependency:go-offline` right after, that (slow, network-bound) layer stays cached across builds where only application code changed — `COPY src ./src` and `mvn clean package` are the only steps that re-run. This is the same caching principle as Q6, applied deliberately: **order Dockerfile instructions from least-frequently-changing to most-frequently-changing.**

## 8. What is `.dockerignore`, and why does it matter?
Like `.gitignore`, but for what gets sent to the Docker daemon as the **build context** (Q28) before the build even starts. This repo's `.dockerignore` excludes `.git`, `.idea`, `target/`, `k8s/`, `learning/`, `interview-questions/`, and `.env*` (except `.env.example`).
```
.env
.env.*
!.env.example
.git
target/
k8s/
learning/
interview-questions/
```
Two reasons this matters: **security** (secrets like a real `.env` must never even reach the build context, let alone get baked into a layer), and **speed** (a smaller context uploads to the daemon faster, and prevents irrelevant file changes — like editing a `.md` file in `interview-questions/`, which has nothing to do with the app — from invalidating build cache unnecessarily).

## 9. `COPY` vs. `ADD` — which should you use?
`COPY` does exactly one thing: copies files/directories from the build context into the image. `ADD` does that *plus* two extra, "magic" behaviors: it can fetch a remote URL, and it auto-extracts local `.tar` archives. The official Docker guidance (and this repo's own Dockerfile) is to default to `COPY` — its behavior is fully predictable, whereas `ADD`'s auto-extraction has caused real production surprises (a `.tar` file accidentally getting extracted when someone only meant to copy it as-is). Reach for `ADD` only for its one genuinely unique feature: extracting a local archive as part of the build.

## 10. What does `EXPOSE` actually do, and how is it different from `-p`/`--publish`?
`EXPOSE 8080` in a Dockerfile is **documentation only** — it tells anyone reading the image (and tools like `docker inspect`) which port the app listens on, but it does **not** open that port to the host. Actually making the port reachable from outside the container requires `-p`/`--publish` at run time:
```bash
docker run -p 8080:8080 shihabmi7/ecommerce-api:latest
#            ^host  ^container
```
Without `-p`, the app inside the container is listening on 8080 just fine, but nothing on the host machine (or outside the container network) can reach it.

## 11. What is a Docker registry? Docker Hub vs. a private registry.
A registry stores and distributes images by name+tag (`shihabmi7/ecommerce-api:latest`). **Docker Hub** is the default public registry (`docker/login-action` + `docker/build-push-action` in this repo's `docker-ci-cd.yml` push there). Organizations with proprietary code typically run a **private registry** instead (AWS ECR, Google Artifact Registry, Azure ACR, or a self-hosted Harbor/Nexus) — same `docker push`/`docker pull` mechanics, just a different, access-controlled endpoint, and images never become publicly pullable by accident.

## 12. Why is tagging an image `:latest` risky in production?
`:latest` is just a regular, mutable tag, not a special "always the newest" marker — whoever pushes last to that tag overwrites what it points to. This repo's own `k8s/app/ecommerce_deployment.yaml` uses `image: shihabmi7/ecommerce-api:latest`, which means: a rollback is hard (there's no way to tell Kubernetes "go back to the image that was `:latest` yesterday" — that information is already gone), and `imagePullPolicy` behavior around `:latest` differs from a real version tag, sometimes re-pulling (or not) in ways that surprise people. The production-safe pattern is to tag every build with something immutable and traceable — a git SHA or semantic version (`ecommerce-api:a1b2c3d` or `:1.4.2`) — so a specific Deployment always points at one specific, unchanging image, and rolling back is just pointing the Deployment at an older tag.

## 13. What is docker-compose, and what problem does it solve?
A single YAML file (`docker-compose.yml`) describing a **multi-container application** — this repo's has three services: `mysql`, `rabbitmq`, and `springboot-app`. Instead of running three separate `docker run` commands by hand (remembering every port mapping, env var, and startup order each time), `docker-compose up` builds/starts all of them together, on a shared network, in one command. It's meant for local development and simple deployments — not a replacement for an orchestrator like Kubernetes at production scale (Q33).

## 14. How do containers in the same docker-compose file talk to each other?
By **service name**, resolved via Docker's built-in DNS on the compose network — not `localhost`, and not the host machine's IP. This repo's `springboot-app` service connects to RabbitMQ with:
```yaml
- SPRING_RABBITMQ_HOST=rabbitmq   # the *service name* in docker-compose.yml, not localhost
```
Inside the `springboot-app` container, `rabbitmq` resolves to whichever container Docker started for the `rabbitmq` service — each container in the compose file can reach every other one by its service name, as if it were a hostname.

## 15. What does `depends_on` actually guarantee — and not guarantee?
It only controls **start order** (container creation order) — it does **not** wait for the dependency to actually be *ready* to accept connections. This repo's `springboot-app` has:
```yaml
depends_on:
  - rabbitmq
  - mysql
```
This guarantees the `rabbitmq`/`mysql` *containers* are started before `springboot-app`'s container starts — but MySQL's container process starting is not the same moment as MySQL actually being ready to accept connections (it still has its own internal startup/init time). A Spring Boot app that tries to connect immediately can still hit a connection refused error in the first few seconds. The real fix is either a retry/backoff on the app side (which Spring Boot's datasource does to a degree) or a proper healthcheck-based wait (`depends_on: condition: service_healthy`, requires a `HEALTHCHECK`, Q24) instead of relying on `depends_on` alone.

## 16. How do you pass configuration/secrets into a container?
**Environment variables** are the standard mechanism (this ties directly to the Twelve-Factor App's config-via-environment principle, Q39). This repo's `docker-compose.yml`:
```yaml
environment:
  - SPRING_DATASOURCE_PASSWORD=root
  - JWT_SECRET_KEY=${JWT_SECRET_KEY}   # pulled from the host shell / a local .env file, never hardcoded
```
`JWT_SECRET_KEY` is deliberately **not** hardcoded in the compose file — it's substituted from whatever's in the shell environment or a local `.env` file (excluded from the build context and from git via `.dockerignore`/`.gitignore`). For real production secrets at scale, a dedicated secret store (Kubernetes `Secret` — Q37, AWS Secrets Manager, Vault) is preferred over plain env vars, since env vars are visible via `docker inspect`/process listing on the host.

## 17. Docker volumes vs. bind mounts — difference and when to use each.
Both let a container persist/share data outside its own writable layer, but **volumes** are managed entirely by Docker (stored under Docker's own directory, portable, independent of host filesystem layout), while **bind mounts** point at a specific path on the host machine directly.
```bash
docker run -v mysql_data:/var/lib/mysql mysql:8.3.0        # named volume — Docker manages the storage
docker run -v ./local-config:/etc/config:ro nginx           # bind mount — a specific host path
```
Volumes are the right default for a database's actual data (portable, works the same on any host); bind mounts are useful for local development (mounting your source code into a container for live-reload) or mounting a specific host config file in.

## 18. Why would a container's data disappear on restart, and how do you prevent it?
A container's own writable layer is **ephemeral** — `docker rm`/recreating the container discards it entirely. This repo's `docker-compose.yml` doesn't declare a named volume for `mysql`'s data directory, which means every `docker-compose down` (or a container recreation) loses all database data. The fix is mounting a volume onto MySQL's data path:
```yaml
mysql:
  image: mysql:8.3.0
  volumes:
    - mysql_data:/var/lib/mysql
volumes:
  mysql_data:
```
Now the actual data lives in the `mysql_data` volume, independent of the container's own lifecycle — recreating the container reattaches to the same data.

## 19. What are Docker networks (bridge, host, none)?
- **bridge** (the default): containers get their own private IP on an isolated virtual network; this is what lets docker-compose services reach each other by name (Q14).
- **host**: the container shares the host machine's network stack directly — no isolation, no port mapping needed, but also no network isolation from the host.
- **none**: the container gets no network access at all — useful for a batch job that only needs the filesystem, never the network.

Compose creates a dedicated bridge network per project by default, which is why this repo's three services (`mysql`, `rabbitmq`, `springboot-app`) can resolve each other by service name without any manual network setup.

## 20. How do you debug a running container?
```bash
docker logs -f ecommerce-api-latest        # stream stdout/stderr from the app
docker exec -it ecommerce-api-latest sh    # get an interactive shell inside the running container
docker inspect ecommerce-api-latest        # full JSON: env vars, mounts, network settings, health status
docker stats ecommerce-api-latest          # live CPU/memory/network usage
```
For a Spring Boot app specifically, `docker logs` is usually the first stop (the same stack trace you'd see running locally), and `docker exec -it ... sh` is useful when you need to check something *inside* the container's filesystem — e.g. confirming an env var actually made it in, or that `app.jar` is where you expect.

## 21. `docker exec` vs. `docker attach` — what's the difference?
`docker exec` starts a **brand-new process** inside an already-running container (e.g. a new shell alongside the app's own main process) — closing it doesn't affect the container's main process at all. `docker attach` instead connects your terminal directly to the container's **existing main process** (PID 1's stdin/stdout/stderr) — and pressing Ctrl+C there can send a signal that kills the container's main process, stopping the whole container. For debugging, `docker exec -it <container> sh` is almost always the safer, more common choice.

## 22. Why does switching a Docker base image from a JDK to a JRE matter?
This is a real change in this repo's history (`bfada96 Fix offline build failure, switch runtime stage to JRE`). The **JDK** includes the compiler (`javac`) and other build-time tools; the **JRE** only includes what's needed to *run* an already-compiled `.jar`. Since this repo's final stage only ever runs `java -jar app.jar` (Q3) — it never compiles anything — shipping the full JDK in the runtime image is pure waste: a larger image (slower pulls/deploys), and a larger attack surface (more binaries an attacker could potentially abuse if they got a shell in the container). `eclipse-temurin:17-jre-jammy` is smaller and sufficient for exactly what the runtime stage needs to do.

## 23. Base image choice — `eclipse-temurin` vs. Alpine vs. distroless. What's the trade-off?
- **`eclipse-temurin:17-jre-jammy`** (this repo's choice): a full Ubuntu-based (glibc) userspace — bigger than Alpine, but maximally compatible (no glibc/musl surprises with native libraries) and easy to `docker exec` into with a normal shell for debugging.
- **Alpine-based** (`eclipse-temurin:17-jre-alpine`): much smaller (Alpine uses musl libc, not glibc), but some Java native libraries/agents historically had subtle compatibility issues on musl — worth testing carefully before switching.
- **Distroless** (Google's `gcr.io/distroless/java17`): no shell, no package manager, nothing but the JRE and the app — smallest attack surface possible, but you lose the ability to `docker exec ... sh` in for debugging entirely.

The right choice depends on what you're optimizing for: this repo's `jre-jammy` prioritizes easy debugging and compatibility over the last few MB of image size — a reasonable default until image size or attack surface actually becomes a measured problem.

## 24. What is a `HEALTHCHECK` instruction, and why doesn't this repo's Dockerfile have one?
`HEALTHCHECK` tells Docker how to actively probe whether the app *inside* the container is actually working — not just whether the process is still running (a hung/deadlocked JVM process still looks "running" to Docker without one).
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1
```
Without it, `docker ps` only shows the container as "Up" based on the process being alive, and `depends_on: condition: service_healthy` (Q15) has nothing to check against. This repo doesn't have one — adding it would let `docker-compose`/an orchestrator distinguish "the JVM process is running" from "the app is actually ready to serve traffic," the same gap Q38's readiness probe closes at the Kubernetes level.

## 25. What are container restart policies?
`--restart` (or `restart:` in compose) tells the Docker daemon what to do when a container's main process exits.
- **`no`** (default): never restart automatically.
- **`on-failure[:max-retries]`**: restart only if it exits with a non-zero (error) status.
- **`always`**: always restart, even after `docker stop` followed by a daemon restart.
- **`unless-stopped`**: like `always`, but respects an explicit manual `docker stop` (won't restart until manually started again).

Neither this repo's `docker-compose.yml` nor its Dockerfile sets one, so the default (`no`) applies — a crash just leaves the container stopped rather than automatically recovering, which is a real gap for a production compose setup (though in Kubernetes, Q34's Pod/Deployment machinery handles this instead).

## 26. How do you limit a container's CPU/memory usage, and why does it matter?
```bash
docker run --memory=512m --cpus=1.0 shihabmi7/ecommerce-api:latest
```
Without limits, a single misbehaving container (a memory leak, a runaway query) can consume all of the host's resources and starve every other container on the same machine — there's no isolation on resource *usage* by default, only on namespace/filesystem. In Kubernetes, this same idea is expressed as `resources.limits`/`resources.requests` per container in a Pod spec — this repo's `k8s/app/ecommerce_deployment.yaml` doesn't set any, meaning a single pod has no ceiling on what it can consume from its node, and the scheduler has no `requests` value to reason about when placing it.

## 27. Why should containers run as a non-root user?
By default, the process inside a container runs as **root** unless told otherwise — and container isolation, while strong, is not a perfect security boundary (a container-escape vulnerability, or a mounted host path, can turn "root inside the container" into a real problem on the host). Best practice is to create and switch to an unprivileged user in the Dockerfile:
```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```
This repo's Dockerfile doesn't do this today — the app runs as root inside the container. It's a common, worthwhile hardening step: an attacker who compromises the running app is then constrained to an unprivileged user's permissions, not root, inside the container.

## 28. What is the "build context," and why can a large one slow down builds?
The build context is the full set of files sent from your machine to the Docker daemon before the build starts — normally the directory you run `docker build .` from. Every `COPY`/`ADD` instruction can only reference files inside that context. A large context (an untrimmed `node_modules/`, `.git/` history, `target/` build output) has to be zipped up and transferred to the daemon on *every single build*, even for files that never get `COPY`'d into the image — this is exactly what `.dockerignore` (Q8) exists to trim down.

## 29. `docker-compose up` vs. `docker-compose up --build` — what's the difference?
`docker-compose up` starts containers from **already-built** images — if `springboot-app`'s image was built once and the Dockerfile/source hasn't changed since, Compose reuses the existing image as-is, even if you've since edited a source file. `docker-compose up --build` forces Compose to **rebuild** any service with a `build:` key (like `springboot-app: build: .` in this repo) before starting it — the command you actually want after changing application code, since otherwise you'd be running a stale image without realizing it.

## 30. What is Docker Buildx, and what problem does multi-arch solve?
Buildx is Docker's extended build engine (`docker/setup-buildx-action` in this repo's `docker-ci-cd.yml`) that, among other things, supports building a single image manifest that works across **multiple CPU architectures** (`linux/amd64`, `linux/arm64`) in one build:
```bash
docker buildx build --platform linux/amd64,linux/arm64 -t shihabmi7/ecommerce-api:latest --push .
```
Without multi-arch, an image built on an Intel/AMD CI runner won't run on an Apple Silicon Mac or an ARM-based cloud instance (AWS Graviton) without emulation. This repo's CI doesn't currently set `--platform`, so it builds for the runner's own architecture (amd64) only — worth knowing as the next natural improvement if the image needs to run on ARM infrastructure.

## 31. Walk through what happens in this repo's CI pipeline, from `git push` to a running container.
`.github/workflows/docker-ci-cd.yml`, on a push/PR to `main`:
```
1. test job:        checkout → set up JDK 17 → ./mvnw test -Dspring.profiles.active=test
2. docker-build-and-push job (needs: test, so it only runs if tests pass):
     checkout → docker/setup-buildx-action → docker/login-action (Docker Hub)
     → docker/build-push-action (builds the Dockerfile, tags :latest, pushes to Docker Hub)
```
Note this is CI (build/test/publish the image) but **not CD** in the "auto-deploy" sense despite the workflow's name — nothing in this workflow actually applies the `k8s/` manifests or restarts a running deployment; the image is published and ready to be pulled, but rolling it out to an actual cluster would need a separate step (`kubectl set image` / `kubectl apply` / an ArgoCD sync, etc.) that isn't part of this file today.

## 32. What is cloud-native development?
An approach to building and running applications designed from the start to run well in dynamic, distributed, cloud environments — rather than a traditional app just lifted onto a cloud VM unchanged. The commonly cited pillars: **containerization** (Docker — package once, run identically anywhere), **microservices** (independently deployable services, `microservices-communication-questions.md`), **dynamic orchestration** (Kubernetes managing scaling/healing/scheduling, Q33), and **CI/CD automation** (Q31's pipeline — build, test, and ship automatically, repeatedly, without manual steps). This repo touches every one of these: it's containerized (`Dockerfile`), has a CI pipeline (`docker-ci-cd.yml`), and has Kubernetes manifests (`k8s/`) ready to deploy it as an orchestrated workload.

## 33. Why do you need an orchestrator like Kubernetes on top of Docker?
Docker (and docker-compose) runs containers on **one machine**. An orchestrator answers the questions that appear the moment you have many containers across many machines: which machine runs which container, what happens when a container crashes (restart it — automatically, without a human), how do you roll out a new version without downtime, how do you scale from 1 to 10 replicas on demand, and how do services find each other across machines whose IPs change constantly. Kubernetes' `Deployment` (Q35) + `Service` (Q36) objects are exactly the abstractions that answer these — this repo's own `k8s/app/ecommerce_deployment.yaml` + `ecommerce_service.yaml` are a (currently minimal, single-replica) example of exactly this.

## 34. What is a Kubernetes Pod, and how does it relate to a Docker container?
A Pod is Kubernetes' smallest deployable unit — usually wrapping **one** container (this repo's `ecommerce` container is the only one in its Pod template), though a Pod can hold multiple tightly-coupled containers that must share the same network namespace and lifecycle (a "sidecar," e.g. a log-shipper next to the main app). Kubernetes never schedules a bare container directly — it always schedules Pods, and Docker (or another container runtime) is just the engine Kubernetes calls underneath to actually run the container(s) inside that Pod.

## 35. Deployment vs. Pod vs. ReplicaSet — how do they relate?
```
Deployment  (you manage this — desired state: "run this image, this many replicas")
    │  creates/manages
    ▼
ReplicaSet  (ensures exactly N Pods matching a label selector exist, replaces any that die)
    │  creates/manages
    ▼
Pod(s)      (the actual running container(s))
```
You essentially never create a Pod or ReplicaSet directly — you declare a `Deployment` (like this repo's `ecommerce_deployment.yaml`, `replicas: 1`), and Kubernetes creates a `ReplicaSet` to satisfy it, which in turn creates the Pod(s). If a Pod crashes, the ReplicaSet notices the count has dropped below what the Deployment asked for and creates a replacement — this is the self-healing Q33 references, and it's also what makes a rolling update work: a new `Deployment` spec creates a *new* ReplicaSet alongside the old one, gradually shifting Pods from old to new.

## 36. What is a Kubernetes Service, and why can't Pods be reached directly?
Pods are ephemeral — they get a new IP every time they're recreated (a crash, a rollout, a reschedule), so nothing should ever hardcode a Pod's IP. A `Service` gives a **stable** virtual IP/DNS name that load-balances traffic across whichever Pods currently match its label selector, regardless of how many times those Pods have been recreated underneath it.
```yaml
# k8s/app/ecommerce_service.yaml (this repo, abbreviated)
apiVersion: v1
kind: Service
spec:
  type: NodePort              # exposes it on a fixed port on every node, not just inside the cluster
  selector:
    app: ecommerce             # matches the Deployment's Pod label
  ports:
    - port: 8080                # the Service's own port, reachable inside the cluster
      targetPort: 8080          # the container port in the Pod
      nodePort: 30036           # reachable from outside the cluster at <node-ip>:30036
```
Other in-cluster services (or an Ingress) talk to `ecommerce-service:8080`, never to an individual Pod's IP — exactly the same DNS-by-name idea as docker-compose service names (Q14), just at cluster scale instead of one host. This repo specifically uses `NodePort` (rather than the more common `ClusterIP`, which is cluster-internal only) so the app is also reachable directly from outside the cluster without needing a separate Ingress/LoadBalancer set up — a reasonable choice for a simple/dev setup, though a real production deployment would more typically put an Ingress or LoadBalancer Service in front instead.

## 37. ConfigMap vs. Secret in Kubernetes — what's the difference?
Both inject configuration into a Pod as env vars or mounted files, without baking values into the image. A **ConfigMap** is for non-sensitive config (a log level, a feature flag); a **Secret** is for sensitive values (passwords, API keys, tokens) — stored base64-encoded (not encrypted by default at rest, unless the cluster has encryption-at-rest configured) and access-controlled separately via RBAC. This repo's `k8s/app/ecommerce_secret.yaml` supplies the app's env vars this way:
```yaml
# ecommerce_deployment.yaml
envFrom:
  - secretRef:
      name: ecommerce-secret
```
This repo's actual `ecommerce_secret.yaml` carries `MYSQL_USER`, `MYSQL_PASSWORD`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, and `JWT_SECRET_KEY` (the last a placeholder value with a comment instructing you to generate a real one via `kubectl create secret` rather than editing the git-tracked file directly) — the Deployment manifest itself never contains a literal secret value, mirroring the same env-var-driven config approach as the local docker-compose setup (Q16).

## 38. Liveness probe vs. readiness probe — and what's missing in this repo's deployment?
- **Liveness probe**: "is this container still alive/functioning?" — if it fails repeatedly, Kubernetes **kills and restarts** the container.
- **Readiness probe**: "is this container ready to receive traffic *right now*?" — if it fails, Kubernetes stops routing Service traffic to that Pod (Q36), but does **not** restart it; the Pod stays up and can become ready again later (e.g. still warming up a cache).
```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
```
This repo's `k8s/app/ecommerce_deployment.yaml` has **neither** — meaning Kubernetes has no way to know if the app has actually finished starting (traffic could be routed to a Pod before Spring Boot has finished initializing), and no way to detect a hung/deadlocked-but-still-running JVM and automatically restart it. Adding both is one of the most impactful small changes this deployment is missing, and it's what a rolling update or a graceful shutdown (referenced in the MCQ file's Q76) actually needs to coordinate against.

## 39. The Twelve-Factor App methodology — which factors does this repo already follow?
A widely-referenced set of practices for building cloud-native, horizontally-scalable apps. A few, checked against this repo directly:
- **III. Config in the environment** — ✅ `JWT_SECRET_KEY`, DB credentials, RabbitMQ host all come from env vars (`application.properties` uses `${...}` placeholders, Q16), never hardcoded.
- **VI. Processes are stateless** — ✅ this app keeps no in-memory session state between requests; auth is a stateless JWT (see `jpa-fetching-questions.md`/security notes), so any replica can serve any request.
- **V. Strictly separate build and run stages** — ✅ exactly what the multi-stage Dockerfile (Q5) does: build once, run the same artifact everywhere.
- **XI. Logs as event streams** — ✅ the app logs to stdout/stderr (`docker logs`, Q20 picks it up directly) rather than managing its own log files.
- **IX. Disposability — fast startup, graceful shutdown** — ⚠️ partial: Spring Boot 3.4's graceful shutdown default helps, but without a readiness probe (Q38) or `terminationGracePeriodSeconds` set in the Deployment, Kubernetes can't fully coordinate a clean rollout.

## 40. Horizontal scaling in Kubernetes — what can break if you just bump `replicas` without checking the app first?
`replicas: 3` in a `Deployment` is easy to change, but it doesn't make an app horizontally scalable on its own — it just runs more copies of whatever the app already is, including whatever *isn't* actually shared correctly. Two concrete failure modes, both already covered elsewhere in these notes:
- **Database connection exhaustion**: each Pod gets its own connection pool (HikariCP default of 10, `relational-db-basics.md` Q26/Q40) — `replicas x pool size` must stay under Postgres's `max_connections`, or new Pods start failing to connect the moment you scale out.
- **Per-instance state that should be shared**: an in-memory rate limiter or cache counted *per pod* effectively multiplies its limit by the replica count, since each Pod tracks its own count independently (the same shape of bug as the MCQ file's Q80) — anything meant to be a single, global limit has to live in a shared store (Redis) instead of a JVM field, or it silently stops meaning what it's supposed to mean the moment there's more than one replica.

This repo's `k8s/app/ecommerce_deployment.yaml` currently runs `replicas: 1` — scaling it up is a one-line change, but doing so safely means first confirming nothing in the app (or its Hikari pool sizing) implicitly assumed "there's only ever one of me."

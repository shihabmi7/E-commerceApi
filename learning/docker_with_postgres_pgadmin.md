# PostgreSQL and pgAdmin4 Docker Setup

```bash
# Create a custom Docker network
docker network create my-network

# Run pgAdmin4 container

docker run -d \
  --name pgadmin \
  --network my-network \
  -e PGADMIN_DEFAULT_EMAIL=admin@admin.com \
  -e PGADMIN_DEFAULT_PASSWORD=shihab \
  -p 5050:80 \
  dpage/pgadmin4

# Run PostgreSQL container
docker run -d \
  --name postgres \
  --network my-network \
  -e POSTGRES_PASSWORD=shihab \
  -p 5432:5432 \
  postgres

# Test network connectivity between containers
docker exec -it pgadmin ping postgres

# network inspection
docker network inspect my-network

# create Database
create database database name


# 30 Essential Docker Commands

---
## **Basic Commands**

| Command                          | Description                                      |
|----------------------------------|--------------------------------------------------|
| `docker --version`               | Check the installed Docker version               |
| `docker pull <image>`            | Download an image from Docker Hub                 |
| `docker run <image>`             | Run a container from an image                    |
| `docker ps`                      | List running containers                          |
| `docker ps -a`                   | List all containers (including stopped ones)     |

---
## **Running and Managing Images**

| Command                          | Description                                      |
|----------------------------------|--------------------------------------------------|
| `docker run -d <image>`          | Run a container in detached mode                 |
| `docker run -it <image> /bin/bash` | Run a container interactively with a shell     |
| `docker run -p 8080:80 <image>`  | Run a container and map host port 8080 to container port 80 |
| `docker run --name <name> <image>` | Run a container with a custom name             |
| `docker run -v /host/path:/container/path <image>` | Run a container with a volume mount |
| `docker run --env VAR=value <image>` | Run a container with environment variables   |
| `docker run --rm <image>`        | Run a container and remove it after it exits     |
| `docker inspect <container_id>`  | Inspect a container's configuration and details  |
| `docker port <container_id>`     | List port mappings for a container               |
| `docker top <container_id>`      | Display running processes in a container         |

---
## **Container Management**

| Command                          | Description                                      |
|----------------------------------|--------------------------------------------------|
| `docker stop <container_id>`     | Stop a running container                         |
| `docker start <container_id>`    | Start a stopped container                         |
| `docker restart <container_id>`  | Restart a container                               |
| `docker rm <container_id>`       | Remove a stopped container                        |
| `docker logs <container_id>`     | View logs of a container                          |

---
## **Image Management**

| Command                          | Description                                      |
|----------------------------------|--------------------------------------------------|
| `docker images`                  | List all downloaded images                        |
| `docker rmi <image_id>`          | Remove an image                                   |
| `docker build -t <name> .`       | Build an image from a Dockerfile in the current directory |
| `docker history <image>`         | Show the history of an image                      |
| `docker tag <image> <repository>/<tag>` | Tag an image for pushing to a repository |

---
## **Advanced Commands**

| Command                          | Description                                      |
|----------------------------------|--------------------------------------------------|
| `docker exec -it <container> bash` | Enter a running container in interactive mode   |
| `docker-compose up`              | Start services defined in `docker-compose.yml`  |
| `docker stats`                   | Display live resource usage statistics for containers |
| `docker network ls`              | List all Docker networks                         |
| `docker system prune`            | Remove unused containers, networks, and images  |

---
## **MVN Commands**


.PHONY: keys dev docker-up docker-down test build clean reset init

keys:
	@./scripts/generate-keys.sh

dev:
	docker compose up postgres -d && ./mvnw quarkus:dev

docker-up: build
	docker compose up --build -d
	@echo "API running at http://localhost:8080"
	@echo "Swagger UI:   http://localhost:8080/swagger-ui"

docker-down:
	docker compose down

test:
	./mvnw test

build:
	./mvnw package -DskipTests

clean:
	./mvnw clean

reset: clean
	docker compose down -v

init:
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo ".env created from .env.example — update DB_PASSWORD if needed."; \
	else \
		echo ".env already exists, skipping"; \
	fi
	@./scripts/generate-keys.sh
	./mvnw package -DskipTests
	docker compose up --build

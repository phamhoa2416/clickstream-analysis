# Clickstream Analysis Platform

This repository contains the source code for the back-end of the Clickstream Analysis Platform, a real-time analytics stack built with Java (Spring Boot), Apache Kafka, Apache Spark, ClickHouse, and a modern monitoring/visualization suite. Recommended to run and inspect with [IntelliJ IDEA](https://www.jetbrains.com/idea/) or your preferred Java IDE.

---

### Tools & Techstack

<p>
  <img alt="java" src="https://img.shields.io/badge/-Java-007396?style=for-the-badge&logo=java&logoColor=white"/>
  <img alt="spring-boot" src="https://img.shields.io/badge/-Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
  <img alt="kafka" src="https://img.shields.io/badge/-Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white"/>
  <img alt="spark" src="https://img.shields.io/badge/-Spark-E25A1C?style=for-the-badge&logo=apachespark&logoColor=white"/>
  <img alt="clickhouse" src="https://img.shields.io/badge/-ClickHouse-FFDD00?style=for-the-badge&logo=clickhouse&logoColor=black"/>
  <img alt="prometheus" src="https://img.shields.io/badge/-Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white"/>
  <img alt="grafana" src="https://img.shields.io/badge/-Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"/>
  <img alt="superset" src="https://img.shields.io/badge/-Superset-1A73E8?style=for-the-badge&logo=apache-superset&logoColor=white"/>
  <img alt="docker" src="https://img.shields.io/badge/-Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img alt="intellij" src="https://img.shields.io/badge/-IntelliJ%20IDEA-ff6c5b?style=for-the-badge&logo=intellij-idea&logoColor=white"/>
</p>

---

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) & [Docker Compose](https://docs.docker.com/compose/install/) for containerized deployment
- (Optional) Java 11+ and Maven 3.8+ for manual build/run
- (Optional) Python 3.7+ for custom Superset requirements
- (Optional) ClickHouse client for direct DB access
- (Optional) Kafka client tools for topic inspection

---

### Quick Start (Recommended)

1. **Clone the Repository**
   ```
   git clone <your-repo-url>
   cd clickstream-analysis
   ```

2. **Configure Environment Variables**
   Create a `.env` file in the project root (or export these variables in your shell):
   ```env
   CONFLUENT_VERSION=7.4.0
   CLICKHOUSE_DB=clickstream
   CLICKHOUSE_USER=default
   CLICKHOUSE_PASSWORD=yourpassword
   GF_SECURITY_ADMIN_USER=admin
   GF_SECURITY_ADMIN_PASSWORD=admin
   SUPERSET_SECRET_KEY=your_superset_secret
   ```

3. **Start All Services**
   ```
   docker-compose up --build
   ```
   This will start all services (Kafka, ClickHouse, Spring Boot app, Nginx, Prometheus, Grafana, Superset, etc.)

4. **Access the Platform**
   - **API Gateway (Nginx):** http://localhost/
   - **Spring Boot API:** http://localhost:8080/
   - **Kafka UI:** http://localhost:8090/
   - **ClickHouse UI:** http://localhost:8123/
   - **Prometheus:** http://localhost:9090/
   - **Grafana:** http://localhost:3000/ (login with admin/admin)
   - **Superset:** http://localhost:8088/ (login with admin/admin)

---

### Configuration

- **Application Properties**
  Edit `src/main/resources/application.properties` to adjust:
  - Kafka brokers, topics, consumer group
  - ClickHouse connection details
  - Spark settings
  - Monitoring endpoints

- **Nginx**
  Edit `nginx/conf.d/default.conf` to change proxy rules or CORS settings.

- **Prometheus**
  Edit `src/main/resources/prometheus.yml` to add scrape targets.

---

### Manual Build & Run (Advanced)

1. **Build the Java Application**
   ```
   mvn clean package
   ```
   The JAR will be in `target/clickstream-analysis-1.0-SNAPSHOT.jar`.

2. **Run the Application**
   ```
   java -jar target/clickstream-analysis-1.0-SNAPSHOT.jar
   ```

3. **Python Requirements (for Superset or ClickHouse scripts)**
   ```
   pip install -r requirements.txt
   ```

---

### Project Structure

- _**General project structure**_
    ```
    ├── src/
        ├── main/ - Main project code
            ├── java/com/example/ - Primary package of this app
                ├── clickstream/ - Clickstream ingestion, processing, and business logic
                ├── monitoring/ - Monitoring, metrics, and health endpoints
                ├── Application.java - Entry point of the app
            ├── resources/ - Application configs, static files, Prometheus config, etc.
        ├── test/ - Unit and integration tests
    ├── grafana/ - Grafana data and dashboards
    ├── kafka/ - Kafka config
    ├── nginx/ - Nginx config and logs
    ├── prometheus/ - Prometheus data
    ├── superset/ - Superset custom files
    ├── Dockerfile - Script to generate a Docker image for this application
    ├── docker-compose.yml - Orchestrates all services
    ├── pom.xml - Maven build file & dependencies
    ├── requirements.txt - Python requirements for analytics/visualization
    ├── README.md - This document
    ```

- _**Module structure**_

    The backbone of this app comprises several modules, each with their respective purpose and functionality. These modules implement aspects of the layered architecture (config, controller, service, model, etc.).

    ```
    ├── clickstream/
        ├── config/ - Application and Kafka/Spark/ClickHouse config
        ├── controller/ - REST API endpoints for clickstream events
        ├── models/ - Data models for events and storage
        ├── producer/ - Kafka producer logic
        ├── processor/ - Spark streaming and processing logic
        ├── service/ - Business logic and service layer
        ├── validation/ - Input validation
        ├── exception/ - Custom exception handling
    ├── monitoring/
        ├── config/ - Monitoring config
        ├── controller/ - Monitoring endpoints
        ├── health/ - Health checks
        ├── metrics/ - Prometheus metrics integration
    ```

---

> _**Notes:** This repository provides a Docker Compose setup for local and cloud deployment. For advanced configuration, refer to the comments in each config file._

---

## License
MIT (or specify your license)

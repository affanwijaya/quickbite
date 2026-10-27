# ---------- Tahap 1: build file .jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Unduh dependency dulu agar build berikutnya lebih cepat (cache layer)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

# ---------- Tahap 2: image kecil untuk menjalankan aplikasi ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ordering-system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

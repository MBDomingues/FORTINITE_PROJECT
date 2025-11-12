# 1. Imagem base para compilar o projeto (Maven com Java 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Compila o projeto e gera o .jar
RUN mvn clean package -DskipTests

# 2. Imagem final, otimizada para rodar (Apenas Java 21)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copia o .jar gerado na etapa anterior
COPY --from=build /app/target/fortnite-store-api-0.0.1-SNAPSHOT.jar app.jar
# Expõe a porta que o Spring usa
EXPOSE 8080

# Comando para iniciar a aplicação
# O perfil ativo (SPRING_PROFILES_ACTIVE=docker) será pego do docker-compose.yml
# As configurações de inicialização agora são controladas pelo application-docker.properties
ENTRYPOINT ["java","-jar","/app/app.jar"]
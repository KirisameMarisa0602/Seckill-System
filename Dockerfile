# 多阶段构建后端镜像：先用 JDK 打包，再把 fat jar 拷进精简 JRE 运行。
# 构建上下文为仓库根目录；前端静态资源不打进该镜像。

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
# 先拉依赖，后续源码变更可复用这一层缓存
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# 非 root 运行，降低容器权限面
RUN useradd --system --uid 10001 spring
COPY --from=build /workspace/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

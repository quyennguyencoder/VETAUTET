# Hướng dẫn triển khai (Deploy) dự án Spring Boot lên AWS EC2 bằng Docker

Tài liệu này là một template hướng dẫn chuẩn để bạn có thể tự tay đóng gói bất kỳ dự án Java Spring Boot nào thành Docker Image và triển khai lên máy chủ (AWS EC2, VPS, Ubuntu...). Quá trình này mô phỏng lại cách các hệ thống CI/CD (như GitHub Actions, Jenkins) hoạt động.

---

## Môi trường yêu cầu (Local Machine)
- Máy tính đã cài đặt **Java** và **Maven** (hoặc Gradle).
- Đã cài đặt **Docker Desktop** và đã đăng nhập.
- Có một tài khoản Docker Hub (ví dụ: `your_dockerhub_username`).

## Môi trường yêu cầu (Server)
- Máy chủ Ubuntu/Linux đã cài đặt **Docker** và **Docker Compose**.
- Đã có file Key (ví dụ `key.pem`) để kết nối SSH vào server.
- Đã mở các Port ứng dụng cần thiết trên tường lửa (Security Groups của AWS).

---

## Bước 1: Build mã nguồn ra file JAR (Tại Local)
Mở Terminal/PowerShell tại thư mục gốc của dự án Spring Boot (nơi chứa file `pom.xml` hoặc `build.gradle`) và tiến hành đóng gói:

**Nếu dùng Maven:**
```bash
mvn clean package -DskipTests
```
**Nếu dùng Gradle:**
```bash
./gradlew clean build -x test
```
> **Kết quả mong đợi:** Một file `.jar` sẽ được sinh ra nằm trong thư mục `target/` (đối với Maven) hoặc `build/libs/` (đối với Gradle).

---

## Bước 2: Chuẩn bị Dockerfile
Đảm bảo tại thư mục gốc dự án có một file `Dockerfile` được cấu hình chuẩn bị sẵn. Dưới đây là mẫu Dockerfile tối ưu cho Spring Boot:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Đổi đường dẫn file jar bên dưới cho đúng với project của bạn
COPY target/<ten-file-app>.jar app.jar
EXPOSE <cong-ung-dung>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Bước 3: Build Docker Image và Push lên kho chứa (Tại Local)

### 3.1. Đăng nhập Docker Hub
```bash
docker login
```

### 3.2. Build Image
Chạy lệnh sau tại thư mục chứa `Dockerfile`. Đặt tên image tuỳ ý (ví dụ `<project-name>`):
*(Lưu ý dấu chấm `.` ở cuối lệnh)*
```bash
docker build -t <your_dockerhub_username>/<project-name>:latest .
```

### 3.3. Push Image lên mạng
Đẩy image vừa build lên Docker Hub để server có thể tải về:
```bash
docker push <your_dockerhub_username>/<project-name>:latest
```

---

## Bước 4: Triển khai ứng dụng trên Server (AWS EC2)

### 4.1. SSH vào máy chủ
Mở terminal và kết nối vào máy chủ EC2 của bạn:
```bash
ssh -i "path/to/your-key.pem" ubuntu@<dia-chi-ip-ec2>
```

### 4.2. Khởi động hạ tầng (Database, Cache, Message Broker...)
Nếu dự án yêu cầu MySQL, Redis, Kafka... bạn cần khởi động chúng trước. Thông thường bạn sẽ đẩy một file `docker-compose.yml` lên server và chạy:
```bash
docker compose up -d
```

### 4.3. Kéo (Pull) Image ứng dụng về EC2
```bash
docker pull <your_dockerhub_username>/<project-name>:latest
```

### 4.4. Khởi động Backend Application
Chạy container backend bằng lệnh sau (đổi các giá trị `<...>` cho phù hợp):

```bash
docker run -d \
  --name <project-name>-app \
  --network host \
  --restart unless-stopped \
  -p <port-server>:<port-container> \
  <your_dockerhub_username>/<project-name>:latest
```
*(Ghi chú: Cờ `--network host` thường được dùng để container ứng dụng dễ dàng gọi tới các dịch vụ DB/Redis đang map ở `localhost` trên cùng server đó).*

---

## Bước 5: Kiểm tra và Gỡ lỗi (Troubleshooting)

### Kiểm tra log của ứng dụng
Để biết ứng dụng có khởi động thành công hay bị lỗi khi kết nối Database:
```bash
docker logs -f <project-name>-app
```
*(Ấn Ctrl+C để thoát chế độ xem log).*

### Kiểm tra bằng lệnh cURL
Gọi thử một API cơ bản hoặc API Health-check (nếu có dùng Actuator) ngay trên server:
```bash
curl http://localhost:<port>/actuator/health
```

### Truy cập từ bên ngoài
Mở trình duyệt: `http://<dia-chi-ip-ec2>:<port>`. 
Nếu web không tải được, hãy kiểm tra lại cấu hình **Inbound Rules** trong **Security Groups** của AWS Console và đảm bảo port đó đã được cho phép (Allow).

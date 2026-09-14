# DocuLib

DocuLib là ứng dụng web quản lý phát triển và biên mục nguồn tài liệu dùng chung, xây dựng bằng Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security, Thymeleaf và MySQL.

## Chức năng chính

- Dashboard thống kê và danh sách tài liệu được sắp xếp theo lần cập nhật gần nhất.
- Biên mục, tra cứu, lọc, phân trang, yêu thích, chia sẻ vào danh sách người nhận và thùng rác mềm.
- Quy trình phát triển nguồn bắt buộc đi lần lượt: **Mới đề xuất → Đang thẩm định → Đã phê duyệt → Đã đặt mua → Đã tiếp nhận**.
- Có thể từ chối ở bước Mới đề xuất hoặc Đang thẩm định; lý do từ chối là bắt buộc.
- Nhật ký chuyển trạng thái và nhật ký tạo, sửa, chia sẻ, gỡ chia sẻ, xóa mềm, khôi phục tài liệu.
- Khóa phiên bản lạc quan để cảnh báo khi hai người cùng sửa; bộ đếm có khóa database để tránh trùng mã `TL-...` và `BS-...`.
- Đăng nhập BCrypt, CSRF, phân quyền theo vai trò và buộc đăng nhập lại khi quyền, mật khẩu hoặc trạng thái tài khoản thay đổi.
- Giao diện sáng/tối responsive, font Be Vietnam Pro (fallback Arial, Helvetica, sans-serif), sidebar mobile có quản lý focus bằng bàn phím.

## Ma trận quyền

DocuLib là **kho biên mục dùng chung**. Mọi tài khoản đang hoạt động và đã đăng nhập đều xem được tất cả tài liệu chưa bị xóa. “Chia sẻ” chỉ đưa tài liệu vào danh sách **Được chia sẻ** của người nhận, không cấp hoặc thu hồi quyền xem tài liệu.

| Chức năng | Quản trị viên | Cán bộ biên mục | Người xem |
| --- | :---: | :---: | :---: |
| Xem dashboard, kho và chi tiết tài liệu | Có | Có | Có |
| Yêu thích, xem danh sách được chia sẻ | Có | Có | Có |
| Tạo/sửa tài liệu, chia sẻ, phát triển nguồn | Có | Có | Không |
| Xem nhật ký nghiệp vụ | Có | Có | Không |
| Xóa mềm, khôi phục, xóa vĩnh viễn | Có | Không | Không |
| Dữ liệu danh mục, tài khoản và phân quyền | Có | Không | Không |

Hệ thống không cho vô hiệu hóa tài khoản đang sử dụng và luôn phải còn ít nhất một quản trị viên hoạt động.

## Cấu hình theo môi trường

| Profile | Mục đích | Dữ liệu mẫu | Tự mở trình duyệt | Thymeleaf cache | Schema |
| --- | --- | --- | --- | --- | --- |
| `dev` (mặc định) | Eclipse/VS Code/máy cá nhân | Bật, có thể tắt | Bật, có thể tắt | Tắt | Flyway migrate + JPA validate |
| `migration-test` | Bản sao MySQL để thử migration | Tắt | Tắt | Bật | Flyway migrate + JPA validate |
| `prod` | Môi trường thật | Tắt | Tắt | Bật | Flyway migrate + JPA validate |
| `test` | Kiểm thử Maven | Tắt | Tắt | Tắt | H2 in-memory, không dùng MySQL cá nhân |

Production bắt buộc nhận `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` từ biến môi trường và không có mật khẩu database mặc định.

## Chuẩn bị MySQL

Yêu cầu Java 21, MySQL Server 8.x và Eclipse có Maven Integration (m2e).

1. Mở MySQL Workbench bằng tài khoản `root`.
2. Chạy [database/setup.sql](database/setup.sql) để tạo schema `doculib_library` và tài khoản dev.
3. Chọn đúng một trường hợp:

### Database mới, chưa có bảng

Database mới của bản phục hồi là `doculib_library`. Flyway được bật mặc định ở profile dev, tự tạo schema V1 rồi nâng lên V2 trong lần chạy đầu; các lần sau chỉ kiểm tra hoặc chạy migration mới.

### Database đã có dữ liệu

Không chạy ứng dụng mới trực tiếp lên database đó. Trước tiên:

1. Sao lưu database.
2. Khôi phục sang một schema thử nghiệm riêng.
3. Baseline bản sao ở V1, chạy V2 và đối chiếu số dòng tài liệu, tài khoản, yêu thích, chia sẻ.
4. Chỉ lặp lại trên database thật khi bản sao đã đạt yêu cầu.

Lệnh cụ thể và truy vấn đối chiếu nằm trong [database/MIGRATION.md](database/MIGRATION.md). Dự án cố ý đặt `baseline-on-migrate=false` và profile dev không tự sửa schema.

## Chạy bằng Eclipse

1. Import hoặc mở project **DocuLib**.
2. Chọn project → **Maven → Update Project...**.
3. Bảo đảm **Installed JRE** và **Compiler compliance** là Java 21.
4. Cấu hình các biến môi trường khi cần:

   - `DB_URL` (mặc định dev: `jdbc:mysql://localhost:3306/doculib_library?...`)
   - `DB_USERNAME` (mặc định dev: `doculib_user`)
   - `DB_PASSWORD` (mặc định dev: `doculib_password`)
   - `FLYWAY_ENABLED=false` chỉ khi cần tạm thời vô hiệu hóa migration để chẩn đoán
   - `OPEN_BROWSER=false` nếu không muốn tự mở web
   - `APP_SEED_DATA=false` nếu không muốn dữ liệu minh họa
   - `DOCULIB_SEED_DEFAULT_USERS=false` nếu không muốn tạo tài khoản mẫu

5. Mở `src/main/java/vn/edu/doculib/DocuLibApplication.java` → **Run As → Java Application** (hoặc **Spring Boot App**).
6. Chờ dòng `Started DocuLibApplication`, sau đó mở `http://localhost:8080`.

Tài khoản mẫu chỉ dành cho profile dev:

| Tên đăng nhập | Mật khẩu ban đầu | Vai trò |
| --- | --- | --- |
| `admin` | `Admin@123` | Quản trị viên |
| `librarian` | `BienMuc@123` | Cán bộ biên mục |
| `viewer` | `BanDoc@123` | Người xem |

Đổi mật khẩu ngay sau lần đăng nhập đầu. Có thể thay mật khẩu khởi tạo bằng `DOCULIB_ADMIN_PASSWORD`, `DOCULIB_LIBRARIAN_PASSWORD`, `DOCULIB_VIEWER_PASSWORD`.

## Build và chạy bằng dòng lệnh

Dự án có Maven Wrapper, không cần cài Maven riêng:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

Nếu terminal macOS chưa nhận Java 21:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean verify
```

Chạy production:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://db-host:3306/doculib_library?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh'
export DB_USERNAME='doculib_app'
export DB_PASSWORD='mat-khau-lay-tu-kho-bi-mat'
./mvnw spring-boot:run
```

Profile prod không tạo dữ liệu/tài khoản mẫu và không tự mở trình duyệt.

## Kiểm thử và CI

```bash
./mvnw clean verify
```

Các test dùng H2 in-memory ở chế độ tương thích MySQL và tách khỏi database cá nhân. Bộ test bao gồm phân quyền phiên đang hoạt động, quy trình trạng thái, lý do từ chối, nhật ký, che dữ liệu bí mật, xung đột chỉnh sửa, URL quay lại nội bộ, sinh mã đồng thời và dựng schema Flyway trên database thử nghiệm cô lập.

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy `verify` bằng Java 21 khi mã nguồn sau này được đưa lên GitHub. File chỉ là cấu hình; dự án này không tự commit, push hoặc kích hoạt dịch vụ bên ngoài.

## Dependency bổ sung

- `flyway-core`: quản lý và kiểm chứng thứ tự thay đổi schema.
- `flyway-mysql`: hỗ trợ Flyway nhận diện và migrate MySQL.
- `flyway-maven-plugin`: cung cấp các lệnh baseline/migrate thủ công trong hướng dẫn nâng cấp database.

Không có frontend framework mới, upload/download file, AI, OCR hay microservice trong phạm vi bản nâng cấp này.

# Nâng cấp database an toàn

DocuLib dùng Flyway cho database mới và production. Hồ sơ `dev` trỏ tới database mới `doculib_library` và bật Flyway mặc định. Không trỏ cấu hình này vào database cũ nếu chưa sao lưu và kiểm chứng migration trên bản sao.

## Trước khi nâng cấp

1. Dừng DocuLib để không có giao dịch đang ghi.
2. Sao lưu bằng MySQL Workbench (**Server → Data Export**) hoặc:

   ```bash
   mysqldump --single-transaction --routines --triggers \
     -u root -p doculib_library > doculib_library_before_flyway.sql
   ```

3. Khôi phục bản sao sang một schema thử nghiệm, ví dụ `doculib_library_migration_test`.
4. Đếm và đối chiếu các bảng quan trọng trước/sau: `resource_materials`, `user_accounts`, `material_favorites`, `material_shares`.

## Database đã có dữ liệu

Migration V1 mô tả schema cũ; V2 chỉ thêm cột khóa phiên bản, bảng sinh mã và các bảng nhật ký. Với một schema cũ chưa có `flyway_schema_history`, hãy baseline **bản sao** ở phiên bản 1 rồi mới migrate:

```bash
./mvnw -Dflyway.url="$MIGRATION_DB_URL" \
  -Dflyway.user="$MIGRATION_DB_USERNAME" \
  -Dflyway.password="$MIGRATION_DB_PASSWORD" \
  -Dflyway.baselineVersion=1 flyway:baseline

./mvnw -Dflyway.url="$MIGRATION_DB_URL" \
  -Dflyway.user="$MIGRATION_DB_USERNAME" \
  -Dflyway.password="$MIGRATION_DB_PASSWORD" flyway:migrate
```

Chỉ lặp lại trên database thật sau khi đã kiểm tra bản sao, giữ file backup và có thời gian bảo trì. Không bật `baseline-on-migrate` tự động.

## Kiểm tra sau migration

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT COUNT(*) FROM resource_materials;
SELECT COUNT(*) FROM user_accounts;
SELECT COUNT(*) FROM material_favorites;
SELECT COUNT(*) FROM material_shares;
```

Nếu có lỗi, không sửa tay bảng lịch sử Flyway. Giữ nguyên database gốc, xem lỗi trên bản sao và chỉ khôi phục backup khi đã xác định đúng nguyên nhân.

-- Chạy file này một lần bằng MySQL Workbench với tài khoản root.
-- Có thể đổi mật khẩu ở đây, sau đó đổi DB_PASSWORD trong Run Configuration của Eclipse.

CREATE DATABASE IF NOT EXISTS doculib_library
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'doculib_user'@'localhost'
    IDENTIFIED BY 'doculib_password';

CREATE USER IF NOT EXISTS 'doculib_user'@'127.0.0.1'
    IDENTIFIED BY 'doculib_password';

ALTER USER 'doculib_user'@'localhost'
    IDENTIFIED BY 'doculib_password';

ALTER USER 'doculib_user'@'127.0.0.1'
    IDENTIFIED BY 'doculib_password';

GRANT ALL PRIVILEGES ON doculib_library.* TO 'doculib_user'@'localhost';
GRANT ALL PRIVILEGES ON doculib_library.* TO 'doculib_user'@'127.0.0.1';

-- Kiểm tra database đã được tạo:
SHOW DATABASES LIKE 'doculib_library';
SHOW GRANTS FOR 'doculib_user'@'localhost';

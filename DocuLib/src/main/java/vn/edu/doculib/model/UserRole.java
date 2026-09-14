package vn.edu.doculib.model;

public enum UserRole {
    ADMIN("Quản trị viên", "Toàn quyền cấu hình, tài khoản và dữ liệu", "danger"),
    LIBRARIAN("Cán bộ biên mục", "Tạo và cập nhật tài liệu, đề xuất bổ sung", "primary"),
    VIEWER("Người xem", "Tra cứu và xem thông tin tài liệu", "neutral");

    private final String label;
    private final String description;
    private final String badgeClass;

    UserRole(String label, String description, String badgeClass) {
        this.label = label;
        this.description = description;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }
}

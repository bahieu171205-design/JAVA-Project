package vn.edu.doculib.model;

public enum MaterialStatus {
    AVAILABLE("Sẵn sàng", "success"),
    PROCESSING("Đang xử lý", "warning"),
    LIMITED("Hạn chế", "info"),
    ARCHIVED("Lưu trữ", "neutral");

    private final String label;
    private final String badgeClass;

    MaterialStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}

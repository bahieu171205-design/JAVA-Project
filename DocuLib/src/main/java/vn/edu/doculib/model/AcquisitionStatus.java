package vn.edu.doculib.model;

public enum AcquisitionStatus {
    PROPOSED("Mới đề xuất", "info"),
    REVIEWING("Đang thẩm định", "warning"),
    APPROVED("Đã phê duyệt", "success"),
    ORDERED("Đã đặt mua", "primary"),
    RECEIVED("Đã tiếp nhận", "success"),
    REJECTED("Từ chối", "danger");

    private final String label;
    private final String badgeClass;

    AcquisitionStatus(String label, String badgeClass) {
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

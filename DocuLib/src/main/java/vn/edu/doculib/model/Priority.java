package vn.edu.doculib.model;

public enum Priority {
    LOW("Thấp", "neutral"),
    MEDIUM("Trung bình", "info"),
    HIGH("Cao", "warning"),
    URGENT("Khẩn", "danger");

    private final String label;
    private final String badgeClass;

    Priority(String label, String badgeClass) {
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

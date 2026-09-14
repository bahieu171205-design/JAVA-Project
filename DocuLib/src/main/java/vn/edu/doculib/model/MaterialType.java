package vn.edu.doculib.model;

public enum MaterialType {
    BOOK("Sách"),
    JOURNAL("Tạp chí"),
    THESIS("Luận văn / luận án"),
    REPORT("Báo cáo"),
    EBOOK("Tài liệu điện tử"),
    AUDIO_VISUAL("Nghe nhìn"),
    OTHER("Khác");

    private final String label;

    MaterialType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

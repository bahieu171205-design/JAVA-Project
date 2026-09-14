package vn.edu.doculib.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MaterialForm {

    private Long id;

    private Long version;

    @Size(max = 40, message = "Mã tài liệu tối đa 40 ký tự")
    private String inventoryCode;

    @NotBlank(message = "Nhan đề không được để trống")
    @Size(max = 300, message = "Nhan đề tối đa 300 ký tự")
    private String title;

    @Size(max = 30, message = "ISBN/ISSN tối đa 30 ký tự")
    private String isbnIssn;

    @NotNull(message = "Loại tài liệu không được để trống")
    private MaterialType materialType;

    @NotNull(message = "Trạng thái không được để trống")
    private MaterialStatus status;

    @Size(max = 80, message = "Ngôn ngữ tối đa 80 ký tự")
    private String language;

    @Min(value = 1000, message = "Năm xuất bản phải từ 1000")
    @Max(value = 2100, message = "Năm xuất bản không hợp lệ")
    private Integer publishYear;

    @Size(max = 80, message = "Lần xuất bản tối đa 80 ký tự")
    private String edition;

    @Size(max = 80, message = "Ký hiệu xếp giá tối đa 80 ký tự")
    private String callNumber;

    @Min(value = 1, message = "Số trang phải từ 1")
    private Integer pages;

    @NotNull(message = "Số bản không được để trống")
    @Min(value = 0, message = "Số bản không được âm")
    private Integer quantity;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    private Long categoryId;
    private Long publisherId;
    private Set<Long> authorIds = new LinkedHashSet<>();

    public static MaterialForm fromEntity(ResourceMaterial material) {
        MaterialForm form = new MaterialForm();
        form.setId(material.getId());
        form.setVersion(material.getVersion());
        form.setInventoryCode(material.getInventoryCode());
        form.setTitle(material.getTitle());
        form.setIsbnIssn(material.getIsbnIssn());
        form.setMaterialType(material.getMaterialType());
        form.setStatus(material.getStatus());
        form.setLanguage(material.getLanguage());
        form.setPublishYear(material.getPublishYear());
        form.setEdition(material.getEdition());
        form.setCallNumber(material.getCallNumber());
        form.setPages(material.getPages());
        form.setQuantity(material.getQuantity());
        form.setDescription(material.getDescription());
        if (material.getCategory() != null) {
            form.setCategoryId(material.getCategory().getId());
        }
        if (material.getPublisher() != null) {
            form.setPublisherId(material.getPublisher().getId());
        }
        form.setAuthorIds(material.getAuthors().stream()
                .map(author -> author.getId())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getInventoryCode() {
        return inventoryCode;
    }

    public void setInventoryCode(String inventoryCode) {
        this.inventoryCode = inventoryCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbnIssn() {
        return isbnIssn;
    }

    public void setIsbnIssn(String isbnIssn) {
        this.isbnIssn = isbnIssn;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public MaterialStatus getStatus() {
        return status;
    }

    public void setStatus(MaterialStatus status) {
        this.status = status;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public Set<Long> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(Set<Long> authorIds) {
        this.authorIds = authorIds == null ? new LinkedHashSet<>() : authorIds;
    }
}

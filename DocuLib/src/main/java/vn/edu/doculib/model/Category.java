package vn.edu.doculib.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã chủ đề không được để trống")
    @Size(max = 30, message = "Mã chủ đề tối đa 30 ký tự")
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 150, message = "Tên chủ đề tối đa 150 ký tự")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Column(length = 500)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

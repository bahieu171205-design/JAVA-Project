package vn.edu.doculib.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_shares",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_share_recipient",
                columnNames = {"material_id", "shared_with_id"}))
public class MaterialShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private ResourceMaterial material;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private UserAccount sharedWith;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private UserAccount sharedBy;

    @Column(name = "shared_at", nullable = false, updatable = false)
    private LocalDateTime sharedAt;

    @PrePersist
    void onCreate() {
        sharedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ResourceMaterial getMaterial() {
        return material;
    }

    public void setMaterial(ResourceMaterial material) {
        this.material = material;
    }

    public UserAccount getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(UserAccount sharedWith) {
        this.sharedWith = sharedWith;
    }

    public UserAccount getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(UserAccount sharedBy) {
        this.sharedBy = sharedBy;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }
}

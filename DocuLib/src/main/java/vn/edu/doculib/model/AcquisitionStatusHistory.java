package vn.edu.doculib.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "acquisition_status_history")
public class AcquisitionStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "request_code", nullable = false, length = 40)
    private String requestCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private AcquisitionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private AcquisitionStatus toStatus;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(length = 1000)
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public AcquisitionStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(AcquisitionStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public AcquisitionStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(AcquisitionStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}

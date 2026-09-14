package vn.edu.doculib.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.exception.InvalidAcquisitionTransitionException;
import vn.edu.doculib.exception.ResourceNotFoundException;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.repository.AcquisitionRequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AcquisitionService {

    private final AcquisitionRequestRepository requestRepository;
    private final BusinessCodeService businessCodeService;
    private final AcquisitionAuditService auditService;

    public AcquisitionService(AcquisitionRequestRepository requestRepository,
                              BusinessCodeService businessCodeService,
                              AcquisitionAuditService auditService) {
        this.requestRepository = requestRepository;
        this.businessCodeService = businessCodeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AcquisitionRequest> search(String query, AcquisitionStatus status, int page, int size) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        return requestRepository.search(normalizedQuery, status,
                PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Transactional(readOnly = true)
    public AcquisitionRequest getById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất #" + id));
    }

    @Transactional
    public AcquisitionRequest save(AcquisitionRequest input, String actorUsername) {
        boolean creating = input.getId() == null;
        AcquisitionRequest request;
        if (creating) {
            request = new AcquisitionRequest();
            request.setRequestCode(businessCodeService.nextAcquisitionCode());
        } else {
            request = getById(input.getId());
            if (!Objects.equals(input.getVersion(), request.getVersion())) {
                throw new ConcurrentUpdateException(
                        "Đề xuất đã được người khác cập nhật. Hãy tải lại trang và kiểm tra trạng thái mới nhất.");
            }
        }

        AcquisitionStatus previousStatus = request.getStatus();
        AcquisitionStatus requestedStatus = creating
                ? AcquisitionStatus.PROPOSED
                : (input.getStatus() == null ? previousStatus : input.getStatus());
        String transitionNote = previousStatus == requestedStatus
                ? null
                : normalizeTransitionNote(requestedStatus, input.getNote());
        if (!creating && previousStatus != requestedStatus) {
            validateTransition(previousStatus, requestedStatus, input.getNote());
        }

        request.setProposedTitle(input.getProposedTitle().trim());
        request.setRequester(input.getRequester().trim());
        request.setReason(input.getReason().trim());
        request.setQuantity(input.getQuantity());
        request.setEstimatedPrice(input.getEstimatedPrice());
        request.setPriority(input.getPriority());
        request.setStatus(requestedStatus);
        request.setNote(StringUtils.hasText(input.getNote()) ? input.getNote().trim() : null);
        try {
            AcquisitionRequest saved = requestRepository.saveAndFlush(request);
            if (creating) {
                auditService.record(saved, null, AcquisitionStatus.PROPOSED, actorUsername,
                        StringUtils.hasText(saved.getNote()) ? saved.getNote() : "Tạo đề xuất bổ sung");
            } else if (previousStatus != requestedStatus) {
                auditService.record(saved, previousStatus, requestedStatus, actorUsername, transitionNote);
            }
            return saved;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentUpdateException(
                    "Đề xuất đã được người khác cập nhật. Hãy tải lại trang và thử lại.", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "Không thể sinh mã đề xuất duy nhất do có thao tác đồng thời. Vui lòng thử lại.", exception);
        }
    }

    @Transactional
    public void updateStatus(Long id, AcquisitionStatus status, String note, Long version, String actorUsername) {
        AcquisitionRequest request = getById(id);
        if (!Objects.equals(version, request.getVersion())) {
            throw new ConcurrentUpdateException(
                    "Trạng thái đề xuất vừa được người khác thay đổi. Hãy tải lại danh sách.");
        }
        AcquisitionStatus previousStatus = request.getStatus();
        validateTransition(previousStatus, status, note);
        request.setStatus(status);
        request.setNote(StringUtils.hasText(note) ? note.trim() : request.getNote());
        try {
            requestRepository.saveAndFlush(request);
            auditService.record(request, previousStatus, status, actorUsername,
                    normalizeTransitionNote(status, note));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentUpdateException(
                    "Trạng thái đề xuất vừa được người khác thay đổi. Hãy tải lại danh sách.", exception);
        }
    }

    @Transactional
    public void delete(Long id) {
        requestRepository.delete(getById(id));
    }

    @Transactional(readOnly = true)
    public List<AcquisitionStatus> availableStatuses(AcquisitionStatus current) {
        if (current == null) {
            return List.of(AcquisitionStatus.PROPOSED);
        }
        List<AcquisitionStatus> statuses = new ArrayList<>();
        statuses.add(current);
        AcquisitionStatus next = nextStatus(current);
        if (next != null) {
            statuses.add(next);
        }
        if (current == AcquisitionStatus.PROPOSED || current == AcquisitionStatus.REVIEWING) {
            statuses.add(AcquisitionStatus.REJECTED);
        }
        return List.copyOf(statuses);
    }

    private void validateTransition(AcquisitionStatus from, AcquisitionStatus to, String note) {
        if (from == null || to == null || from == to) {
            throw new InvalidAcquisitionTransitionException("Hãy chọn trạng thái tiếp theo của quy trình");
        }
        if (to == AcquisitionStatus.REJECTED) {
            if (from != AcquisitionStatus.PROPOSED && from != AcquisitionStatus.REVIEWING) {
                throw new InvalidAcquisitionTransitionException(
                        "Chỉ có thể từ chối đề xuất ở bước Mới đề xuất hoặc Đang thẩm định");
            }
            if (!StringUtils.hasText(note)) {
                throw new InvalidAcquisitionTransitionException("Phải nhập lý do khi từ chối đề xuất");
            }
            return;
        }
        if (nextStatus(from) != to) {
            throw new InvalidAcquisitionTransitionException(
                    "Không thể chuyển trực tiếp từ “" + from.getLabel() + "” sang “" + to.getLabel() + "”");
        }
    }

    private AcquisitionStatus nextStatus(AcquisitionStatus status) {
        return switch (status) {
            case PROPOSED -> AcquisitionStatus.REVIEWING;
            case REVIEWING -> AcquisitionStatus.APPROVED;
            case APPROVED -> AcquisitionStatus.ORDERED;
            case ORDERED -> AcquisitionStatus.RECEIVED;
            case RECEIVED, REJECTED -> null;
        };
    }

    private String normalizeTransitionNote(AcquisitionStatus target, String note) {
        return StringUtils.hasText(note) ? note.trim() : "Chuyển sang " + target.getLabel();
    }
}

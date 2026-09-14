package vn.edu.doculib;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.exception.InvalidAcquisitionTransitionException;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.model.Priority;
import vn.edu.doculib.repository.AcquisitionStatusHistoryRepository;
import vn.edu.doculib.service.AcquisitionService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcquisitionWorkflowTests {

    @Autowired
    private AcquisitionService acquisitionService;

    @Autowired
    private AcquisitionStatusHistoryRepository historyRepository;

    @Test
    void newRequestAlwaysStartsAtProposedAndCannotSkipAWorkflowStep() {
        AcquisitionRequest created = acquisitionService.save(newRequest(AcquisitionStatus.APPROVED), "librarian.qa");

        assertThat(created.getStatus()).isEqualTo(AcquisitionStatus.PROPOSED);
        assertThat(historyRepository.findAllByRequestIdOrderByChangedAtDesc(created.getId()))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getFromStatus()).isNull();
                    assertThat(entry.getToStatus()).isEqualTo(AcquisitionStatus.PROPOSED);
                    assertThat(entry.getActorUsername()).isEqualTo("librarian.qa");
                });

        AcquisitionRequest edited = copyForEdit(created);
        edited.setStatus(AcquisitionStatus.ORDERED);
        assertThatThrownBy(() -> acquisitionService.save(edited, "librarian.qa"))
                .isInstanceOf(InvalidAcquisitionTransitionException.class)
                .hasMessageContaining("Không thể chuyển trực tiếp");
    }

    @Test
    void sequentialTransitionsAreRecordedAndRejectionRequiresAReason() {
        AcquisitionRequest created = acquisitionService.save(newRequest(AcquisitionStatus.PROPOSED), "creator.qa");

        assertThatThrownBy(() -> acquisitionService.updateStatus(
                created.getId(), AcquisitionStatus.REJECTED, " ", created.getVersion(), "reviewer.qa"))
                .isInstanceOf(InvalidAcquisitionTransitionException.class)
                .hasMessageContaining("lý do");

        acquisitionService.updateStatus(created.getId(), AcquisitionStatus.REVIEWING,
                "Đã tiếp nhận hồ sơ", created.getVersion(), "reviewer.qa");
        AcquisitionRequest reviewing = acquisitionService.getById(created.getId());
        acquisitionService.updateStatus(reviewing.getId(), AcquisitionStatus.APPROVED,
                "Hội đồng đồng ý", reviewing.getVersion(), "manager.qa");

        List<?> history = historyRepository.findAllByRequestIdOrderByChangedAtDesc(created.getId());
        assertThat(history).hasSize(3);
        assertThat(historyRepository.findAllByRequestIdOrderByChangedAtDesc(created.getId()).getFirst().getNote())
                .isEqualTo("Hội đồng đồng ý");
    }

    @Test
    void auditTextRedactsSecrets() {
        AcquisitionRequest created = acquisitionService.save(newRequest(AcquisitionStatus.PROPOSED), "creator.qa");
        acquisitionService.updateStatus(created.getId(), AcquisitionStatus.REVIEWING,
                "password=khong-duoc-luu", created.getVersion(), "reviewer.qa");

        String note = historyRepository.findAllByRequestIdOrderByChangedAtDesc(created.getId()).getFirst().getNote();
        assertThat(note).contains("password=[đã ẩn]").doesNotContain("khong-duoc-luu");
    }

    @Test
    void staleAcquisitionEditIsRejected() {
        AcquisitionRequest created = acquisitionService.save(newRequest(AcquisitionStatus.PROPOSED), "creator.qa");
        AcquisitionRequest staleCopy = copyForEdit(created);
        acquisitionService.updateStatus(created.getId(), AcquisitionStatus.REVIEWING,
                "Bắt đầu thẩm định", created.getVersion(), "reviewer.qa");

        staleCopy.setProposedTitle("Dữ liệu cũ không được ghi đè");
        assertThatThrownBy(() -> acquisitionService.save(staleCopy, "creator.qa"))
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("người khác cập nhật");
    }

    private AcquisitionRequest newRequest(AcquisitionStatus submittedStatus) {
        AcquisitionRequest request = new AcquisitionRequest();
        request.setProposedTitle("Tài liệu kiểm thử quy trình");
        request.setRequester("Phòng kiểm thử");
        request.setReason("Bổ sung cho chương trình đào tạo");
        request.setQuantity(2);
        request.setPriority(Priority.MEDIUM);
        request.setStatus(submittedStatus);
        return request;
    }

    private AcquisitionRequest copyForEdit(AcquisitionRequest source) {
        AcquisitionRequest copy = new AcquisitionRequest();
        copy.setId(source.getId());
        copy.setVersion(source.getVersion());
        copy.setProposedTitle(source.getProposedTitle());
        copy.setRequester(source.getRequester());
        copy.setReason(source.getReason());
        copy.setQuantity(source.getQuantity());
        copy.setEstimatedPrice(source.getEstimatedPrice());
        copy.setPriority(source.getPriority());
        copy.setStatus(source.getStatus());
        copy.setNote(source.getNote());
        return copy;
    }
}

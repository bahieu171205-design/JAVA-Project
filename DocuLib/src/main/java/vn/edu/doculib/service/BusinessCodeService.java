package vn.edu.doculib.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.CodeSequence;
import vn.edu.doculib.repository.AcquisitionRequestRepository;
import vn.edu.doculib.repository.CodeSequenceRepository;
import vn.edu.doculib.repository.ResourceMaterialRepository;

import java.time.Year;
import java.util.Locale;
import java.util.function.Predicate;

@Service
public class BusinessCodeService {

    private static final String MATERIAL_SEQUENCE = "MATERIAL";
    private static final String ACQUISITION_SEQUENCE = "ACQUISITION";

    private final CodeSequenceRepository sequenceRepository;
    private final ResourceMaterialRepository materialRepository;
    private final AcquisitionRequestRepository acquisitionRepository;

    public BusinessCodeService(CodeSequenceRepository sequenceRepository,
                               ResourceMaterialRepository materialRepository,
                               AcquisitionRequestRepository acquisitionRepository) {
        this.sequenceRepository = sequenceRepository;
        this.materialRepository = materialRepository;
        this.acquisitionRepository = acquisitionRepository;
    }

    @Transactional
    public synchronized String nextMaterialCode() {
        return nextCode(MATERIAL_SEQUENCE, "TL", materialRepository::existsByInventoryCodeIgnoreCase);
    }

    @Transactional
    public synchronized String nextAcquisitionCode() {
        return nextCode(ACQUISITION_SEQUENCE, "BS", acquisitionRepository::existsByRequestCode);
    }

    private String nextCode(String sequenceName, String prefix, Predicate<String> alreadyExists) {
        int currentYear = Year.now().getValue();
        CodeSequence sequence = sequenceRepository.findForUpdate(sequenceName)
                .orElseThrow(() -> new IllegalStateException(
                        "Thiếu bộ đếm mã " + sequenceName + "; hãy kiểm tra migration database"));
        if (sequence.getCurrentYear() != currentYear) {
            sequence.setCurrentYear(currentYear);
            sequence.setNextValue(1);
        }

        String code;
        do {
            code = prefix + "-" + currentYear + "-"
                    + String.format(Locale.ROOT, "%04d", sequence.getNextValue());
            sequence.setNextValue(sequence.getNextValue() + 1);
        } while (alreadyExists.test(code));
        sequenceRepository.save(sequence);
        return code;
    }

}

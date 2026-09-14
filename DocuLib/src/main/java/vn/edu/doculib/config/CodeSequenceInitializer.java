package vn.edu.doculib.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.CodeSequence;
import vn.edu.doculib.repository.CodeSequenceRepository;

@Component
@Order(-100)
public class CodeSequenceInitializer implements ApplicationRunner {

    private final CodeSequenceRepository sequenceRepository;

    public CodeSequenceInitializer(CodeSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("MATERIAL");
        createIfMissing("ACQUISITION");
    }

    private void createIfMissing(String name) {
        if (sequenceRepository.existsById(name)) {
            return;
        }
        CodeSequence sequence = new CodeSequence();
        sequence.setSequenceName(name);
        sequence.setCurrentYear(0);
        sequence.setNextValue(1);
        sequenceRepository.save(sequence);
    }
}

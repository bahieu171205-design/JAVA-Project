package vn.edu.doculib;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.doculib.service.BusinessCodeService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BusinessCodeConcurrencyTests {

    @Autowired
    private BusinessCodeService codeService;

    @Test
    void concurrentRequestsReceiveDistinctMaterialAndAcquisitionCodes() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> materialTasks = new ArrayList<>();
            List<Callable<String>> acquisitionTasks = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                materialTasks.add(codeService::nextMaterialCode);
                acquisitionTasks.add(codeService::nextAcquisitionCode);
            }

            List<String> materialCodes = values(executor.invokeAll(materialTasks));
            List<String> acquisitionCodes = values(executor.invokeAll(acquisitionTasks));

            assertThat(new HashSet<>(materialCodes)).hasSize(20);
            assertThat(new HashSet<>(acquisitionCodes)).hasSize(20);
            assertThat(materialCodes).allMatch(code -> code.matches("TL-\\d{4}-\\d{4,}"));
            assertThat(acquisitionCodes).allMatch(code -> code.matches("BS-\\d{4}-\\d{4,}"));
        } finally {
            executor.shutdownNow();
        }
    }

    private List<String> values(List<Future<String>> futures) throws Exception {
        List<String> values = new ArrayList<>();
        for (Future<String> future : futures) {
            values.add(future.get());
        }
        return values;
    }
}

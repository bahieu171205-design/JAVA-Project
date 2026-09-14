package vn.edu.doculib.dto;

import java.math.BigDecimal;

public record DashboardStats(
        long materialTitles,
        long totalCopies,
        long availableTitles,
        long processingTitles,
        long limitedTitles,
        long archivedTitles,
        long pendingAcquisitions,
        long proposedAcquisitions,
        long reviewingAcquisitions,
        long approvedAcquisitions,
        long orderedAcquisitions,
        long receivedAcquisitions,
        BigDecimal approvedBudget,
        int availablePercent,
        int processingPercent,
        int limitedPercent,
        int archivedPercent,
        int catalogQualityPercent
) {
}

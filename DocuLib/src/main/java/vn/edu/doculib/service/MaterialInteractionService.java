package vn.edu.doculib.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.exception.ResourceNotFoundException;
import vn.edu.doculib.model.MaterialFavorite;
import vn.edu.doculib.model.MaterialAuditAction;
import vn.edu.doculib.model.MaterialShare;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.repository.MaterialFavoriteRepository;
import vn.edu.doculib.repository.MaterialShareRepository;
import vn.edu.doculib.repository.UserAccountRepository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MaterialInteractionService {

    private final MaterialFavoriteRepository favoriteRepository;
    private final MaterialShareRepository shareRepository;
    private final UserAccountRepository userAccountRepository;
    private final MaterialService materialService;
    private final MaterialAuditService auditService;

    public MaterialInteractionService(MaterialFavoriteRepository favoriteRepository,
                                      MaterialShareRepository shareRepository,
                                      UserAccountRepository userAccountRepository,
                                      MaterialService materialService,
                                      MaterialAuditService auditService) {
        this.favoriteRepository = favoriteRepository;
        this.shareRepository = shareRepository;
        this.userAccountRepository = userAccountRepository;
        this.materialService = materialService;
        this.auditService = auditService;
    }

    @Transactional
    public boolean toggleFavorite(Long materialId, String username) {
        ResourceMaterial material = materialService.getById(materialId);
        var existing = favoriteRepository.findByUserUsernameIgnoreCaseAndMaterialId(username, materialId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }

        MaterialFavorite favorite = new MaterialFavorite();
        favorite.setUser(getAccount(username));
        favorite.setMaterial(material);
        favoriteRepository.save(favorite);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long materialId, String username) {
        return favoriteRepository.existsByUserUsernameIgnoreCaseAndMaterialId(username, materialId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getFavoriteMaterialIds(String username, Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> favoriteIds = new LinkedHashSet<>();
        favoriteRepository.findAllByUserUsernameIgnoreCaseAndMaterialIdIn(username, materialIds)
                .forEach(favorite -> favoriteIds.add(favorite.getMaterial().getId()));
        return favoriteIds;
    }

    @Transactional(readOnly = true)
    public List<MaterialFavorite> findFavorites(String username) {
        return favoriteRepository
                .findAllByUserUsernameIgnoreCaseAndMaterialTrashedFalseOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<MaterialShare> findSharedWith(String username) {
        return shareRepository
                .findAllBySharedWithUsernameIgnoreCaseAndMaterialTrashedFalseOrderBySharedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<MaterialShare> findSharesForMaterial(Long materialId) {
        return shareRepository.findAllByMaterialIdOrderBySharedAtDesc(materialId);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findEligibleRecipients(Long materialId, String currentUsername) {
        Set<Long> sharedUserIds = new LinkedHashSet<>();
        findSharesForMaterial(materialId).forEach(share -> sharedUserIds.add(share.getSharedWith().getId()));
        return userAccountRepository.findAllByOrderByFullNameAsc().stream()
                .filter(UserAccount::isEnabled)
                .filter(user -> !user.getUsername().equalsIgnoreCase(currentUsername))
                .filter(user -> !sharedUserIds.contains(user.getId()))
                .toList();
    }

    @Transactional
    public boolean share(Long materialId, Long recipientId, String sharedByUsername) {
        ResourceMaterial material = materialService.getById(materialId);
        UserAccount sharedBy = getAccount(sharedByUsername);
        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhận chia sẻ"));
        if (!recipient.isEnabled()) {
            throw new IllegalArgumentException("Không thể chia sẻ cho tài khoản đã bị khóa");
        }
        if (recipient.getId().equals(sharedBy.getId())) {
            throw new IllegalArgumentException("Không thể chia sẻ tài liệu cho chính bạn");
        }
        if (shareRepository.findByMaterialIdAndSharedWithId(materialId, recipientId).isPresent()) {
            return false;
        }

        MaterialShare share = new MaterialShare();
        share.setMaterial(material);
        share.setSharedWith(recipient);
        share.setSharedBy(sharedBy);
        shareRepository.saveAndFlush(share);
        auditService.record(material, MaterialAuditAction.SHARED, sharedByUsername,
                "Đưa tài liệu vào danh sách Được chia sẻ của " + recipient.getUsername());
        return true;
    }

    @Transactional
    public void revokeShare(Long materialId, Long recipientId, String actorUsername) {
        MaterialShare share = shareRepository.findByMaterialIdAndSharedWithId(materialId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt chia sẻ cần thu hồi"));
        ResourceMaterial material = share.getMaterial();
        String recipientUsername = share.getSharedWith().getUsername();
        shareRepository.delete(share);
        shareRepository.flush();
        auditService.record(material, MaterialAuditAction.SHARE_REVOKED, actorUsername,
                "Gỡ tài liệu khỏi danh sách Được chia sẻ của " + recipientUsername);
    }

    private UserAccount getAccount(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }
}

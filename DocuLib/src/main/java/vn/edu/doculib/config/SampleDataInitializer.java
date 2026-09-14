package vn.edu.doculib.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.model.Author;
import vn.edu.doculib.model.Category;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.Priority;
import vn.edu.doculib.model.Publisher;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.repository.AcquisitionRequestRepository;
import vn.edu.doculib.repository.AuthorRepository;
import vn.edu.doculib.repository.CategoryRepository;
import vn.edu.doculib.repository.PublisherRepository;
import vn.edu.doculib.repository.ResourceMaterialRepository;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class SampleDataInitializer implements ApplicationRunner {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ResourceMaterialRepository materialRepository;
    private final AcquisitionRequestRepository acquisitionRepository;

    public SampleDataInitializer(AuthorRepository authorRepository,
                                 CategoryRepository categoryRepository,
                                 PublisherRepository publisherRepository,
                                 ResourceMaterialRepository materialRepository,
                                 AcquisitionRequestRepository acquisitionRepository) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.materialRepository = materialRepository;
        this.acquisitionRepository = acquisitionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (materialRepository.count() > 0 || authorRepository.count() > 0) {
            return;
        }

        Author nguyenVanA = author("Nguyễn Văn An", "Việt Nam", "Chuyên gia quản trị tri thức");
        Author tranMinh = author("Trần Minh Hà", "Việt Nam", "Nghiên cứu khoa học dữ liệu");
        Author robertMartin = author("Robert C. Martin", "Hoa Kỳ", "Tác giả lĩnh vực kỹ nghệ phần mềm");
        Author yuvalHarari = author("Yuval Noah Harari", "Israel", "Sử gia và tác giả");
        authorRepository.saveAll(List.of(nguyenVanA, tranMinh, robertMartin, yuvalHarari));

        Category technology = category("CN", "Công nghệ thông tin", "Khoa học máy tính, dữ liệu và công nghệ số");
        Category management = category("QL", "Quản trị", "Quản trị tổ chức và quản lý tri thức");
        Category social = category("XH", "Khoa học xã hội", "Lịch sử, xã hội và nhân văn");
        categoryRepository.saveAll(List.of(technology, management, social));

        Publisher tre = publisher("Nhà xuất bản Trẻ", "TP. Hồ Chí Minh", "info@nxbtre.com.vn");
        Publisher kimDong = publisher("Nhà xuất bản Kim Đồng", "Hà Nội", "contact@nxbkimdong.com.vn");
        Publisher prenticeHall = publisher("Prentice Hall", "United States", null);
        publisherRepository.saveAll(List.of(tre, kimDong, prenticeHall));

        materialRepository.save(material("TL-2026-0001", "Quản trị nguồn tài liệu trong môi trường số",
                "9786040000001", MaterialType.BOOK, MaterialStatus.AVAILABLE, "Tiếng Việt", 2024,
                "Lần 1", "025.2 NG-A", 286, 5, management, tre, nguyenVanA,
                "Giới thiệu quy trình xây dựng, đánh giá và phát triển nguồn tài liệu số."));
        materialRepository.save(material("TL-2026-0002", "Clean Code: A Handbook of Agile Software Craftsmanship",
                "9780132350884", MaterialType.BOOK, MaterialStatus.AVAILABLE, "English", 2008,
                "1st", "005.1 MAR", 464, 3, technology, prenticeHall, robertMartin,
                "Các nguyên tắc và thực hành viết mã nguồn dễ đọc, dễ bảo trì."));
        materialRepository.save(material("TL-2026-0003", "Dữ liệu mở phục vụ nghiên cứu và đổi mới sáng tạo",
                "ISSN 2734-9152", MaterialType.JOURNAL, MaterialStatus.PROCESSING, "Tiếng Việt", 2025,
                "Số 12", "TCKH 025", 96, 8, technology, kimDong, tranMinh,
                "Chuyên đề về hạ tầng dữ liệu nghiên cứu và khoa học mở."));
        materialRepository.save(material("TL-2026-0004", "Sapiens: Lược sử loài người",
                "9786041129985", MaterialType.BOOK, MaterialStatus.LIMITED, "Tiếng Việt", 2022,
                "Tái bản", "909 HAR", 560, 2, social, tre, yuvalHarari,
                "Khảo cứu lịch sử phát triển của loài người từ thời tiền sử đến hiện đại."));

        if (acquisitionRepository.count() == 0) {
            acquisitionRepository.save(acquisition("BS-2026-0001", "Designing Data-Intensive Applications",
                    "Phòng Nghiên cứu", "Bổ sung tài liệu chuyên sâu cho nhóm dữ liệu", 2,
                    "1250000", Priority.HIGH, AcquisitionStatus.REVIEWING, "Ưu tiên bản in mới nhất"));
            acquisitionRepository.save(acquisition("BS-2026-0002", "Bộ tiêu chuẩn biên mục RDA tiếng Việt",
                    "Tổ Biên mục", "Chuẩn hóa nghiệp vụ biên mục tài nguyên số", 1,
                    "850000", Priority.URGENT, AcquisitionStatus.APPROVED, "Đã có báo giá từ nhà cung cấp"));
            acquisitionRepository.save(acquisition("BS-2026-0003", "Tạp chí Khoa học và Công nghệ Việt Nam",
                    "Phòng Bạn đọc", "Gia hạn ấn phẩm định kỳ năm 2027", 12,
                    "120000", Priority.MEDIUM, AcquisitionStatus.PROPOSED, null));
        }
    }

    private Author author(String name, String country, String note) {
        Author author = new Author();
        author.setName(name);
        author.setCountry(country);
        author.setNote(note);
        return author;
    }

    private Category category(String code, String name, String description) {
        Category category = new Category();
        category.setCode(code);
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    private Publisher publisher(String name, String address, String email) {
        Publisher publisher = new Publisher();
        publisher.setName(name);
        publisher.setAddress(address);
        publisher.setEmail(email);
        return publisher;
    }

    private ResourceMaterial material(String code, String title, String isbn, MaterialType type,
                                      MaterialStatus status, String language, Integer year, String edition,
                                      String callNumber, Integer pages, Integer quantity, Category category,
                                      Publisher publisher, Author author, String description) {
        ResourceMaterial material = new ResourceMaterial();
        material.setInventoryCode(code);
        material.setTitle(title);
        material.setIsbnIssn(isbn);
        material.setMaterialType(type);
        material.setStatus(status);
        material.setLanguage(language);
        material.setPublishYear(year);
        material.setEdition(edition);
        material.setCallNumber(callNumber);
        material.setPages(pages);
        material.setQuantity(quantity);
        material.setCategory(category);
        material.setPublisher(publisher);
        material.setAuthors(new LinkedHashSet<>(List.of(author)));
        material.setDescription(description);
        return material;
    }

    private AcquisitionRequest acquisition(String code, String title, String requester, String reason,
                                           Integer quantity, String price, Priority priority,
                                           AcquisitionStatus status, String note) {
        AcquisitionRequest request = new AcquisitionRequest();
        request.setRequestCode(code);
        request.setProposedTitle(title);
        request.setRequester(requester);
        request.setReason(reason);
        request.setQuantity(quantity);
        request.setEstimatedPrice(new BigDecimal(price));
        request.setPriority(priority);
        request.setStatus(status);
        request.setNote(note);
        return request;
    }
}

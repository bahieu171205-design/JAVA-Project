package vn.edu.doculib.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.doculib.model.Author;
import vn.edu.doculib.model.Category;
import vn.edu.doculib.model.Publisher;
import vn.edu.doculib.repository.AuthorRepository;
import vn.edu.doculib.repository.CategoryRepository;
import vn.edu.doculib.repository.PublisherRepository;

import java.util.Locale;

@Controller
@RequestMapping("/reference-data")
public class ReferenceDataController {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    public ReferenceDataController(AuthorRepository authorRepository,
                                   CategoryRepository categoryRepository,
                                   PublisherRepository publisherRepository) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
    }

    @GetMapping
    public String index(Model model) {
        prepareModel(model);
        return "reference-data/index";
    }

    @PostMapping("/authors")
    public String addAuthor(@Valid @ModelAttribute("newAuthor") Author author,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareModel(model);
            return "reference-data/index";
        }
        author.setName(author.getName().trim());
        authorRepository.save(author);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tác giả");
        return "redirect:/reference-data#authors";
    }

    @PostMapping("/categories")
    public String addCategory(@Valid @ModelAttribute("newCategory") Category category,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            String normalizedCode = category.getCode().trim().toUpperCase(Locale.ROOT);
            if (categoryRepository.existsByCodeIgnoreCase(normalizedCode)) {
                bindingResult.rejectValue("code", "duplicate", "Mã chủ đề đã tồn tại");
            } else {
                category.setCode(normalizedCode);
                category.setName(category.getName().trim());
                categoryRepository.save(category);
                redirectAttributes.addFlashAttribute("successMessage", "Đã thêm chủ đề");
                return "redirect:/reference-data#categories";
            }
        }
        prepareModel(model);
        return "reference-data/index";
    }

    @PostMapping("/publishers")
    public String addPublisher(@Valid @ModelAttribute("newPublisher") Publisher publisher,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareModel(model);
            return "reference-data/index";
        }
        publisher.setName(publisher.getName().trim());
        publisherRepository.save(publisher);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhà xuất bản");
        return "redirect:/reference-data#publishers";
    }

    @PostMapping("/authors/{id}/delete")
    public String deleteAuthor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteReference(() -> authorRepository.deleteById(id), "tác giả", redirectAttributes);
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteReference(() -> categoryRepository.deleteById(id), "chủ đề", redirectAttributes);
    }

    @PostMapping("/publishers/{id}/delete")
    public String deletePublisher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteReference(() -> publisherRepository.deleteById(id), "nhà xuất bản", redirectAttributes);
    }

    private String deleteReference(Runnable deleteAction, String label, RedirectAttributes redirectAttributes) {
        try {
            deleteAction.run();
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa " + label);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể xóa " + label + " đang được sử dụng trong biểu ghi tài liệu");
        }
        return "redirect:/reference-data";
    }

    private void prepareModel(Model model) {
        if (!model.containsAttribute("newAuthor")) {
            model.addAttribute("newAuthor", new Author());
        }
        if (!model.containsAttribute("newCategory")) {
            model.addAttribute("newCategory", new Category());
        }
        if (!model.containsAttribute("newPublisher")) {
            model.addAttribute("newPublisher", new Publisher());
        }
        model.addAttribute("authors", authorRepository.findAllByOrderByNameAsc());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("publishers", publisherRepository.findAllByOrderByNameAsc());
    }
}

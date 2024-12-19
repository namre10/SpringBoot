package com.example.CRUDstore.controllers;

import com.example.CRUDstore.model.Product;
import com.example.CRUDstore.model.ProductDTO;
import com.example.CRUDstore.services.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;
@Controller
@RequestMapping("/products")
public class ProductsController {
    @Autowired
    private ProductsRepository repo;

    @GetMapping({"", "/"})
    public String showProductList(Model model) {
        List<Product> products = repo.findAll();
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("productDto", new ProductDTO());
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute("productDto") ProductDTO productDto,
            BindingResult result
    ) {
        if (productDto.getImageFile() == null || productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "Image Field is required"));
        }

        if (result.hasErrors()) {
            return "products/CreateProduct";
        }

        try {
            String storageFileName = saveImage(productDto.getImageFile());
            Product product = new Product();
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());
            product.setCreatedAt(new Date());
            product.setImageFileName(storageFileName);

            repo.save(product);
        } catch (Exception ex) {
            result.reject("error", "An error occurred while saving the product: " + ex.getMessage());
            return "products/CreateProduct";
        }

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id) {
        try {
            Product product = repo.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("Invalid product ID: " + id));
            ProductDTO productDto = new ProductDTO();
            productDto.setName(product.getName());
            productDto.setBrand(product.getBrand());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setDescription(product.getDescription());

            model.addAttribute("product", product);
            model.addAttribute("productDto", productDto);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(
            Model model,
            @RequestParam int id,
            @Valid @ModelAttribute("productDto") ProductDTO productDto,
            BindingResult result
    ) {
        try {
            Product product = repo.findById(id).orElseThrow(() ->
                    new IllegalArgumentException("Invalid product ID: " + id));

            if (result.hasErrors()) {
                model.addAttribute("product", product);
                return "products/EditProduct";
            }

            if (!productDto.getImageFile().isEmpty()) {
                // Delete old image
                String uploadDir = "public/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());
                Files.deleteIfExists(oldImagePath);

                // Save new image
                String storageFileName = saveImage(productDto.getImageFile());
                product.setImageFileName(storageFileName);
            }

            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());

            repo.save(product);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An error occurred: " + ex.getMessage());
            return "products/EditProduct";
        }

        return "redirect:/products";
    }

    private String saveImage(MultipartFile image) throws Exception {
        String uploadDir = "public/images/";
        String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        try (InputStream inputStream = image.getInputStream()) {
            Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
        }

        return storageFileName;
    }

    @GetMapping("/delete")
    public String deleteProduct(
            @RequestParam int id
    ){
        try {
            Product product = repo.findById(id).get();

            // delete product image
            Path imagePath = Paths.get("public/images/  " + product.getImageFileName());
            try {
                Files.delete(imagePath) ;
            }
            catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }

            // delete product
            repo.delete(product );
        }
        catch (Exception ex){
            System.out.println("Exception:" + ex.getMessage());
        }
        return "redirect:/products";
    }
}

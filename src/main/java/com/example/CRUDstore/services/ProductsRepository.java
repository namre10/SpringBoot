package com.example.CRUDstore.services;

import com.example.CRUDstore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Product, Integer  > {
}

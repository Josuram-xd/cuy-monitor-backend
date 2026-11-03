package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CatalogResponse;
import com.cuymonitor.backend.domain.port.in.ListCatalogOptionsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The lists the dashboard draws its pickers with. They come from the database, never from code. */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final ListCatalogOptionsUseCase listCatalogOptionsUseCase;

    public CatalogController(ListCatalogOptionsUseCase listCatalogOptionsUseCase) {
        this.listCatalogOptionsUseCase = listCatalogOptionsUseCase;
    }

    @GetMapping
    public CatalogResponse catalog() {
        return CatalogResponse.from(listCatalogOptionsUseCase.list());
    }
}

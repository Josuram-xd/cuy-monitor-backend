package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigResponse;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cages/{cageId}/guinea-pigs")
public class GuineaPigController {

    private final ListGuineaPigsUseCase listGuineaPigsUseCase;

    public GuineaPigController(ListGuineaPigsUseCase listGuineaPigsUseCase) {
        this.listGuineaPigsUseCase = listGuineaPigsUseCase;
    }

    @GetMapping
    public List<GuineaPigResponse> list(@PathVariable String cageId) {
        return listGuineaPigsUseCase.listGuineaPigs(cageId).stream().map(GuineaPigResponse::from).toList();
    }
}

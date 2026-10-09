package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigResponse;
import com.cuymonitor.backend.adapter.in.web.dto.RegisterGuineaPigRequest;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cages/{cageId}/guinea-pigs")
public class GuineaPigController {

    private final ListGuineaPigsUseCase listGuineaPigsUseCase;
    private final RegisterGuineaPigUseCase registerGuineaPigUseCase;

    public GuineaPigController(ListGuineaPigsUseCase listGuineaPigsUseCase,
                               RegisterGuineaPigUseCase registerGuineaPigUseCase) {
        this.listGuineaPigsUseCase = listGuineaPigsUseCase;
        this.registerGuineaPigUseCase = registerGuineaPigUseCase;
    }

    @GetMapping
    public List<GuineaPigResponse> list(@PathVariable String cageId) {
        return listGuineaPigsUseCase.listGuineaPigs(cageId).stream().map(GuineaPigResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuineaPigResponse register(@PathVariable String cageId, @Valid @RequestBody RegisterGuineaPigRequest request) {
        return GuineaPigResponse.from(registerGuineaPigUseCase.register(
                new RegisterGuineaPigCommand(cageId, request.name(), request.markColor())));
    }
}

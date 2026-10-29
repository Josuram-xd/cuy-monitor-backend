package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.in.ListCatalogOptionsUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListCatalogOptionsUseCase listCatalogOptionsUseCase;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void returnsTheThreeListsWithTheirLabelAndColor() throws Exception {
        given(listCatalogOptionsUseCase.list()).willReturn(Map.of(
                CatalogKind.MARK_COLOR, List.of(new CatalogOption(CatalogKind.MARK_COLOR, "RED", "Rojo", "#d62828", 1)),
                CatalogKind.COAT_COLOR,
                List.of(new CatalogOption(CatalogKind.COAT_COLOR, "CINNAMON", "Canela", "#c9763d", 6)),
                CatalogKind.BREED, List.of(new CatalogOption(CatalogKind.BREED, "TEDDY", "Teddy", null, 4))));

        mvc.perform(get("/api/v1/catalog").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markColors[0].code").value("RED"))
                .andExpect(jsonPath("$.markColors[0].label").value("Rojo"))
                .andExpect(jsonPath("$.markColors[0].hex").value("#d62828"))
                .andExpect(jsonPath("$.coatColors[0].code").value("CINNAMON"))
                .andExpect(jsonPath("$.breeds[0].code").value("TEDDY"))
                .andExpect(jsonPath("$.breeds[0].hex").doesNotExist());
    }

    @Test
    void aKindWithNoOptionComesBackAsAnEmptyList() throws Exception {
        given(listCatalogOptionsUseCase.list()).willReturn(Map.of());

        mvc.perform(get("/api/v1/catalog").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markColors").isEmpty())
                .andExpect(jsonPath("$.coatColors").isEmpty())
                .andExpect(jsonPath("$.breeds").isEmpty());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/catalog"))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject("3f2a4b5c-0000-4000-8000-000000000001"));
    }
}

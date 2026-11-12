package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.code.RandomCageCodeGenerator;
import com.cuymonitor.backend.adapter.out.persistence.OtpChallengePersistenceAdapter;
import com.cuymonitor.backend.adapter.out.persistence.UserPersistenceAdapter;
import com.cuymonitor.backend.adapter.out.persistence.mapper.OtpChallengePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.OtpChallengeJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.UserJpaRepository;
import com.cuymonitor.backend.adapter.out.security.BCryptPasswordHasher;
import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;
import com.cuymonitor.backend.domain.port.out.PasswordHasher;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Adapter pattern — wiring.</b> The adapters below are plain Java: they carry no {@code @Component}, so the
 * pattern is the class itself (a Target interface, an Adaptee it holds, and the translation between the two) and
 * not an annotation. Spring learns about them here, in the composition root, the same way {@link CageAccessConfig}
 * wires the Proxy and {@link NotificationConfig} the Observer.
 *
 * <p>The persistence adapters that declare {@code @Transactional} are not here: that annotation is a transaction
 * boundary, not wiring, and moving it would change behaviour.
 */
@Configuration
public class AdapterConfig {

    /** <b>Adapter</b> — Target: {@link CageCodeGenerator}. */
    @Bean
    public CageCodeGenerator cageCodeGenerator(CageRepository cages) {
        return new RandomCageCodeGenerator(cages);
    }

    /** <b>Adapter</b> — Target: {@link OtpChallengeRepository}, Adaptee: {@link OtpChallengeJpaRepository}. */
    @Bean
    public OtpChallengeRepository otpChallengeRepository(OtpChallengeJpaRepository jpa,
                                                         OtpChallengePersistenceMapper mapper) {
        return new OtpChallengePersistenceAdapter(jpa, mapper);
    }

    /** <b>Adapter</b> — Target: {@link UserRepository}, Adaptee: {@link UserJpaRepository}. */
    @Bean
    public UserRepository userRepository(UserJpaRepository jpa, UserPersistenceMapper mapper) {
        return new UserPersistenceAdapter(jpa, mapper);
    }

    /** <b>Adapter</b> — Target: {@link PasswordHasher}, Adaptee: Spring Security's {@code BCryptPasswordEncoder}. */
    @Bean
    public PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }
}

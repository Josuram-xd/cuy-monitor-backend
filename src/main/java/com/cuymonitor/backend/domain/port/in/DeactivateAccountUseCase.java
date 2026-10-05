package com.cuymonitor.backend.domain.port.in;

public interface DeactivateAccountUseCase {
    void deactivate(DeactivateAccountCommand deactivateAccountCommand);
}

package com.cuymonitor.backend.adapter.in.web.dto;


// currentPassword may be missing only for an account that has no password (made with Google)
public record DeactivateAccountRequest(String currentPassword) {

    @Override
    public String toString() {
        return "DeactivateAccountRequest[currentPassword=***]";
    }
}

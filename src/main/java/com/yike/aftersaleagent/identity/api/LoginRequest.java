package com.yike.aftersaleagent.identity.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String account, @NotBlank String password) { }

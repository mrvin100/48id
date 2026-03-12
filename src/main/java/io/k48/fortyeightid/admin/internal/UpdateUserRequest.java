package io.k48.fortyeightid.admin.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

record UpdateUserRequest(
        @Size(max = 50, message = "Matricule must not exceed 50 characters")
        String matricule,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 50, message = "Batch must not exceed 50 characters")
        String batch,

        @Size(max = 100, message = "Specialization must not exceed 100 characters")
        String specialization
) {}

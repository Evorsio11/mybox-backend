package com.evorsio.mybox.storage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FileIdRequest {
    @NotNull
    private UUID fileId;
}

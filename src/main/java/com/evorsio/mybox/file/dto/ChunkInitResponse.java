package com.evorsio.mybox.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInitResponse {
    private UUID uploadId;
    private Long chunkSize;
    private Integer totalChunks;
    private LocalDateTime expiresAt;
}

package com.evorsio.mybox.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkProgressResponse {
    private UUID uploadId;
    private String status;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Long uploadedBytes;
    private Double progress;
}

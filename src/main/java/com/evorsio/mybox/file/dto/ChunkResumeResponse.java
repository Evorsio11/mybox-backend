package com.evorsio.mybox.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResumeResponse {
    private UUID uploadId;
    private String originalFileName;
    private Integer totalChunks;
    private List<Integer> uploadedChunks;
    private List<Integer> pendingChunks;
}

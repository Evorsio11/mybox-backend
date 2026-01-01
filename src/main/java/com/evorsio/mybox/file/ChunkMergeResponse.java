package com.evorsio.mybox.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMergeResponse {
    private UUID fileId;
    private String originalFileName;
    private Long fileSize;
    private String fileHash;
}

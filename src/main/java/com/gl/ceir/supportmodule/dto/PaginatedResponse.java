package com.gl.ceir.supportmodule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {
    private List<IssueResponse> data;
    private int currentPage;
    private int totalPages;
    private int numberOfElements;
    private long totalElements;
}

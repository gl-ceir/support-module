package com.gl.ceir.supportmodule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gl.ceir.supportmodule.dto.ClientInfo;
import com.gl.ceir.supportmodule.model.app.IssuesEntity;
import com.gl.ceir.supportmodule.repository.app.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class IssuesService {

    @Value("${pagination-page-limit}")
    private Integer limit;
    @Value("${user-management-url}")
    private String userManagementServiceUrl;

    @Autowired
    private IssueRepository issuesRepository;

    private HttpClient httpClient = HttpClient.newHttpClient();
    private ObjectMapper objectMapper = new ObjectMapper();

    public Page<IssuesEntity> getFilteredIssues(
            String startDate, String endDate, String ticketId, String contactNumber,
            String status, String clientType, Integer page, Integer size, String raisedBy
    ) {
        RequestValidator.validatePagination(page, size, limit);
        Specification<IssuesEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                RequestValidator.validateTimes(startDate, endDate);
                LocalDateTime startDateTime = LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIDNIGHT);
                LocalDateTime endDateTime = LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX);
                predicates.add(criteriaBuilder.between(root.get("createAt"), startDateTime, endDateTime));
            }

            if (ticketId != null) {
                String likePattern = "%" + ticketId + "%";
                predicates.add(criteriaBuilder.like(root.get("ticketId"), likePattern));
            }

            if (contactNumber != null) {
                predicates.add(criteriaBuilder.equal(root.get("msisdn"), contactNumber));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (clientType != null) {
                predicates.add(criteriaBuilder.equal(root.get("userType"), clientType));
            }

            List<String> emails = fetchEmailsFromApi(ClientInfo.getLoggedInUser());
            if (raisedBy != null) {
                if (emails != null) {
                    if (emails.contains(raisedBy)) {
                        predicates.add(criteriaBuilder.equal(root.get("raisedBy"), raisedBy));
                    } else {
                        // If raisedBy doesn't match any emails, return empty page
                        return criteriaBuilder.disjunction();
                    }
                } else {
                    return criteriaBuilder.disjunction();
                }
            } else {
                // Using IN clause instead of adding multiple equal predicates
                CriteriaBuilder.In<String> inClause = criteriaBuilder.in(root.get("raisedBy"));
                emails.forEach(inClause::value);
                predicates.add(inClause);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        try {
            // Apply pagination
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"));

            // Fetch filtered issues
            Page<IssuesEntity> pageResult = issuesRepository.findAll(specification, pageRequest);

            return pageResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private List<String> fetchEmailsFromApi(String clientId) {
        try {
            String url = String.format(userManagementServiceUrl, clientId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}


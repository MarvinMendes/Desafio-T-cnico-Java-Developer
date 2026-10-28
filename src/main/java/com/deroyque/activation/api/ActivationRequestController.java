package com.deroyque.activation.api;

import com.deroyque.activation.api.dto.ActivationRequestPageResponse;
import com.deroyque.activation.api.dto.ActivationRequestResponse;
import com.deroyque.activation.api.dto.CreateActivationRequest;
import com.deroyque.activation.application.CreateActivationRequestService;
import com.deroyque.activation.application.CreateActivationResult;
import com.deroyque.activation.application.GetActivationRequestService;
import com.deroyque.activation.application.ListActivationRequestsService;
import com.deroyque.activation.domain.ActivationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class ActivationRequestController {

    private final CreateActivationRequestService createActivationRequestService;
    private final GetActivationRequestService getActivationRequestService;
    private final ListActivationRequestsService listActivationRequestsService;

    public ActivationRequestController(
            CreateActivationRequestService createActivationRequestService,
            GetActivationRequestService getActivationRequestService,
            ListActivationRequestsService listActivationRequestsService
    ) {
        this.createActivationRequestService = createActivationRequestService;
        this.getActivationRequestService = getActivationRequestService;
        this.listActivationRequestsService = listActivationRequestsService;
    }

    @PostMapping
    public ResponseEntity<ActivationRequestResponse> create(@Valid @RequestBody CreateActivationRequest body) {
        CreateActivationResult result = createActivationRequestService.create(body.toCommand(JwtClientIds.current()));
        ActivationRequestResponse response = ActivationRequestResponse.from(result.request());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ActivationRequestResponse getById(@PathVariable UUID id) {
        return ActivationRequestResponse.from(getActivationRequestService.getById(id, JwtClientIds.current()));
    }

    @GetMapping
    public ActivationRequestPageResponse list(
            @RequestParam(required = false) ActivationStatus status,
            @RequestParam(required = false) String customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ActivationRequestResponse> page = listActivationRequestsService
                .list(JwtClientIds.current(), status, customerId, pageable)
                .map(ActivationRequestResponse::from);
        return ActivationRequestPageResponse.from(page);
    }
}

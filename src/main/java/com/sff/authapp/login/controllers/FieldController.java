package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.field.FieldRequestDto;
import com.sff.authapp.login.dtos.field.FieldResponse;
import com.sff.authapp.login.dtos.field.FieldUpdateRequestDto;
import com.sff.authapp.login.services.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fields")
@RequiredArgsConstructor
@Tag(
        name = "Measurement Fields",
        description = "APIs for managing reusable measurement fields"
)
public class FieldController {

    private final FieldService fieldService;


    // ============================================================
    // CREATE
    // ============================================================

    @Operation(
            summary = "Create measurement field",
            description = """
                    Creates a new reusable measurement field.

                    Examples:
                    Chest, Shoulder, Waist, Hip, Sleeve Length.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Measurement field created successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = FieldResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or duplicate field name"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<FieldResponse> createField(

            @Valid
            @RequestBody
            FieldRequestDto request

    ) {

        FieldResponse response =
                fieldService.createField(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @Operation(
            summary = "Get all measurement fields",
            description = """
                    Returns a paginated list of all available
                    measurement fields.

                    These fields can be selected when creating
                    or updating a tailoring service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Measurement fields retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<FieldResponse>> getAllFields(

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                fieldService.getAllFields(pageable)
        );
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @Operation(
            summary = "Search measurement fields",
            description = """
                    Searches measurement fields by name.

                    Example:
                    keyword=chest
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Page<FieldResponse>> searchFields(

            @Parameter(
                    description = "Text to search for in field names",
                    example = "chest"
            )
            @RequestParam
            String keyword,

            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                fieldService.searchFields(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Operation(
            summary = "Get measurement field by ID",
            description = "Returns a single measurement field by its ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Measurement field retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Measurement field not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/{fieldId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<FieldResponse> getFieldById(

            @Parameter(
                    description = "ID of the measurement field",
                    example = "1"
            )
            @PathVariable
            Long fieldId

    ) {

        return ResponseEntity.ok(
                fieldService.getFieldById(fieldId)
        );
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @Operation(
            summary = "Update measurement field",
            description = """
                    Partially updates a measurement field.

                    Only fields supplied in the request will be updated.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Measurement field updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or duplicate field name"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Measurement field not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @PatchMapping("/{fieldId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<FieldResponse> updateField(

            @Parameter(
                    description = "ID of the measurement field",
                    example = "1"
            )
            @PathVariable
            Long fieldId,

            @Valid
            @RequestBody
            FieldUpdateRequestDto request

    ) {

        return ResponseEntity.ok(
                fieldService.updateField(
                        fieldId,
                        request
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    @Operation(
            summary = "Delete measurement field",
            description = """
                    Deletes a measurement field.

                    A field should only be deleted if it is not being
                    used by any tailoring service.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Measurement field deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Measurement field not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @DeleteMapping("/{fieldId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteField(

            @Parameter(
                    description = "ID of the measurement field",
                    example = "1"
            )
            @PathVariable
            Long fieldId

    ) {

        fieldService.deleteField(fieldId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
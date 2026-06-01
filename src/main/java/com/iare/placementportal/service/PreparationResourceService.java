package com.iare.placementportal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.iare.placementportal.dto.PreparationResourceResponse;
import com.iare.placementportal.dto.PreparationResourceStudentResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.entity.PlacementDrive;
import com.iare.placementportal.entity.PreparationResource;
import com.iare.placementportal.repository.PlacementDriveRepository;
import com.iare.placementportal.repository.PreparationResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class PreparationResourceService {

    private static final long MAX_PDF_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PreparationResourceService.class);
    private static final String RAW_UPLOAD_PATH = "/raw/upload/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final PreparationResourceRepository preparationResourceRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final Cloudinary cloudinary;

    public PreparationResourceService(PreparationResourceRepository preparationResourceRepository,
                                      PlacementDriveRepository placementDriveRepository,
                                      Cloudinary cloudinary) {
        this.preparationResourceRepository = preparationResourceRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.cloudinary = cloudinary;
    }

    public PreparationResourceResponse createResource(Long placementDriveId,
                                                      String resourceTitle,
                                                      String description,
                                                      MultipartFile aptitudePdf,
                                                      MultipartFile codingPdf,
                                                      MultipartFile technicalPdf,
                                                      MultipartFile hrPdf) {
        validateBaseRequest(placementDriveId, resourceTitle);
        validateCreateFiles(aptitudePdf, codingPdf, technicalPdf, hrPdf);

        PlacementDrive placementDrive = findDriveOrThrow(placementDriveId);
        PreparationResource preparationResource = new PreparationResource();
        mapBaseFields(preparationResource, placementDrive, resourceTitle, description);
        LOGGER.info("Preparation resource create requested: driveId={}, resourceTitle='{}', incomingDescription='{}', finalDescription='{}'",
                placementDriveId, resourceTitle, description, preparationResource.getDescription());

        uploadProvidedFiles(preparationResource, placementDrive, aptitudePdf, codingPdf, technicalPdf, hrPdf);
        try {
            PreparationResource savedResource = preparationResourceRepository.save(preparationResource);
            LOGGER.info("Preparation resource create saved: id={}, description='{}'", savedResource.getId(), savedResource.getDescription());
            return toResponse(savedResource);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to save preparation resource metadata after Cloudinary upload for driveId={}.", placementDriveId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF upload succeeded but metadata could not be saved. Check backend logs.");
        }
    }

    @Transactional(readOnly = true)
    public List<PreparationResourceResponse> getAllResources() {
        return preparationResourceRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PreparationResourceStudentResponse> getActiveResourcesForStudents() {
        return preparationResourceRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PreparationResourceResponse> getResourcesByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return preparationResourceRepository.findByPlacementDriveIdOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PreparationResourceStudentResponse> getActiveResourcesByDrive(Long placementDriveId) {
        findDriveOrThrow(placementDriveId);
        return preparationResourceRepository.findByPlacementDriveIdAndActiveTrueOrderByCreatedAtDesc(placementDriveId)
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getStudentPdfResponse(Long id, String type, boolean download) {
        return getPdfResponse(id, type, download, true);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getAdminPdfResponse(Long id, String type, boolean download) {
        return getPdfResponse(id, type, download, false);
    }

    public PreparationResourceResponse updateResource(Long id,
                                                      Long placementDriveId,
                                                      String resourceTitle,
                                                      String description,
                                                      MultipartFile aptitudePdf,
                                                      MultipartFile codingPdf,
                                                      MultipartFile technicalPdf,
                                                      MultipartFile hrPdf) {
        validateBaseRequest(placementDriveId, resourceTitle);

        PreparationResource preparationResource = findResourceOrThrow(id);
        PlacementDrive placementDrive = findDriveOrThrow(placementDriveId);

        String existingDescriptionBefore = preparationResource.getDescription();
        mapBaseFields(preparationResource, placementDrive, resourceTitle, description);
        LOGGER.info("Preparation resource update requested: id={}, driveId={}, resourceTitle='{}', incomingDescription='{}', existingDescriptionBefore='{}', finalDescription='{}'",
                id, placementDriveId, resourceTitle, description, existingDescriptionBefore, preparationResource.getDescription());
        uploadProvidedFiles(preparationResource, placementDrive, aptitudePdf, codingPdf, technicalPdf, hrPdf);
        try {
            PreparationResource savedResource = preparationResourceRepository.save(preparationResource);
            LOGGER.info("Preparation resource update saved: id={}, description='{}'", savedResource.getId(), savedResource.getDescription());
            return toResponse(savedResource);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to update preparation resource metadata for resourceId={} and driveId={}.", id, placementDriveId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF upload succeeded but metadata could not be updated. Check backend logs.");
        }
    }

    public void deleteResource(Long id) {
        PreparationResource preparationResource = findResourceOrThrow(id);
        deleteCloudinaryFile(preparationResource.getAptitudePdfPublicId());
        deleteCloudinaryFile(preparationResource.getCodingPdfPublicId());
        deleteCloudinaryFile(preparationResource.getTechnicalPdfPublicId());
        deleteCloudinaryFile(preparationResource.getHrPdfPublicId());
        preparationResourceRepository.delete(preparationResource);
    }

    public PreparationResourceResponse changeResourceActiveStatus(Long id, boolean active) {
        PreparationResource preparationResource = findResourceOrThrow(id);
        preparationResource.setActive(active);
        return toResponse(preparationResourceRepository.save(preparationResource));
    }

    private void validateBaseRequest(Long placementDriveId, String resourceTitle) {
        if (placementDriveId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placement Drive is required.");
        }
        if (resourceTitle == null || resourceTitle.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource Title is required.");
        }
    }

    private void validateCreateFiles(MultipartFile aptitudePdf,
                                     MultipartFile codingPdf,
                                     MultipartFile technicalPdf,
                                     MultipartFile hrPdf) {
        if (isEmpty(aptitudePdf) && isEmpty(codingPdf) && isEmpty(technicalPdf) && isEmpty(hrPdf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one PDF file must be uploaded.");
        }
    }

    private void mapBaseFields(PreparationResource preparationResource,
                               PlacementDrive placementDrive,
                               String resourceTitle,
                               String description) {
        preparationResource.setPlacementDrive(placementDrive);
        preparationResource.setResourceTitle(resourceTitle.trim());
        String normalizedDescription = normalizeOptional(description);
        if (preparationResource.getId() == null || normalizedDescription != null) {
            preparationResource.setDescription(normalizedDescription);
        }
        if (preparationResource.getActive() == null) {
            preparationResource.setActive(true);
        }
    }

    private void uploadProvidedFiles(PreparationResource preparationResource,
                                     PlacementDrive placementDrive,
                                     MultipartFile aptitudePdf,
                                     MultipartFile codingPdf,
                                     MultipartFile technicalPdf,
                                     MultipartFile hrPdf) {
        if (!isEmpty(aptitudePdf)) {
            UploadedFile uploadedFile = uploadPdf(placementDrive, aptitudePdf, "aptitude-material");
            replaceExistingFile(preparationResource.getAptitudePdfPublicId(), uploadedFile.publicId());
            preparationResource.setAptitudePdfUrl(uploadedFile.secureUrl());
            preparationResource.setAptitudePdfPublicId(uploadedFile.publicId());
        }

        if (!isEmpty(codingPdf)) {
            UploadedFile uploadedFile = uploadPdf(placementDrive, codingPdf, "coding-material");
            replaceExistingFile(preparationResource.getCodingPdfPublicId(), uploadedFile.publicId());
            preparationResource.setCodingPdfUrl(uploadedFile.secureUrl());
            preparationResource.setCodingPdfPublicId(uploadedFile.publicId());
        }

        if (!isEmpty(technicalPdf)) {
            UploadedFile uploadedFile = uploadPdf(placementDrive, technicalPdf, "technical-topics");
            replaceExistingFile(preparationResource.getTechnicalPdfPublicId(), uploadedFile.publicId());
            preparationResource.setTechnicalPdfUrl(uploadedFile.secureUrl());
            preparationResource.setTechnicalPdfPublicId(uploadedFile.publicId());
        }

        if (!isEmpty(hrPdf)) {
            UploadedFile uploadedFile = uploadPdf(placementDrive, hrPdf, "hr-preparation");
            replaceExistingFile(preparationResource.getHrPdfPublicId(), uploadedFile.publicId());
            preparationResource.setHrPdfUrl(uploadedFile.secureUrl());
            preparationResource.setHrPdfPublicId(uploadedFile.publicId());
        }
    }

    private UploadedFile uploadPdf(PlacementDrive placementDrive, MultipartFile file, String resourceTypeName) {
        validatePdfFile(file, resourceTypeName);
        ensureCloudinaryConfigured();

        String folder = "placement-portal/preparation-resources/"
                + slugify(placementDrive.getCompany().getCompanyName())
                + "/"
                + slugify(placementDrive.getDriveTitle());
        String publicId = resourceTypeName + ".pdf";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "folder", folder,
                            "public_id", publicId,
                            "use_filename", false,
                            "unique_filename", false,
                            "overwrite", true
                    )
            );

            String secureUrl = Objects.toString(uploadResult.get("secure_url"), null);
            String uploadedPublicId = Objects.toString(uploadResult.get("public_id"), null);
            String uploadedResourceType = Objects.toString(uploadResult.get("resource_type"), null);

            LOGGER.info(
                    "Cloudinary PDF upload succeeded for driveId={}, resourceType={}: secure_url={}, public_id={}, resource_type={}",
                    placementDrive.getId(),
                    resourceTypeName,
                    secureUrl,
                    uploadedPublicId,
                    uploadedResourceType
            );

            if (secureUrl == null || uploadedPublicId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary did not return a valid PDF upload response.");
            }

            return new UploadedFile(normalizePdfUrl(secureUrl, uploadedPublicId), uploadedPublicId);
        } catch (IOException exception) {
            LOGGER.error("Cloudinary PDF upload failed for company='{}', drive='{}', resourceType='{}', originalFilename='{}'.",
                    placementDrive.getCompany().getCompanyName(),
                    placementDrive.getDriveTitle(),
                    resourceTypeName,
                    file.getOriginalFilename(),
                    exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloudinary upload failed for " + prettifyLabel(resourceTypeName) + ". Check backend logs.");
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected Cloudinary upload failure for company='{}', drive='{}', resourceType='{}', originalFilename='{}'.",
                    placementDrive.getCompany().getCompanyName(),
                    placementDrive.getDriveTitle(),
                    resourceTypeName,
                    file.getOriginalFilename(),
                    exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected Cloudinary upload failure for " + prettifyLabel(resourceTypeName) + ". Check backend logs.");
        }
    }

    private void replaceExistingFile(String existingPublicId, String newPublicId) {
        if (existingPublicId != null && !existingPublicId.equals(newPublicId)) {
            deleteCloudinaryFile(existingPublicId);
        }
    }

    private void deleteCloudinaryFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        ensureCloudinaryConfigured();
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
        } catch (IOException exception) {
            LOGGER.warn("Failed to delete Cloudinary raw file with publicId='{}'. Continuing with DB delete.", publicId, exception);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unexpected error while deleting Cloudinary raw file with publicId='{}'. Continuing with DB delete.", publicId, exception);
        }
    }

    private void validatePdfFile(MultipartFile file, String label) {
        if (isEmpty(file)) {
            return;
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ENGLISH);

        if (!originalFilename.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prettifyLabel(label) + " must have a .pdf file extension.");
        }
        if (!contentType.isBlank() && !contentType.contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prettifyLabel(label) + " must be a PDF file.");
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prettifyLabel(label) + " exceeds the maximum file size of 10MB.");
        }
    }

    private void ensureCloudinaryConfigured() {
        String cloudName = Objects.toString(cloudinary.config.cloudName, "");
        String apiKey = Objects.toString(cloudinary.config.apiKey, "");
        String apiSecret = Objects.toString(cloudinary.config.apiSecret, "");

        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }
    }

    private PreparationResource findResourceOrThrow(Long id) {
        return preparationResourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preparation resource not found."));
    }

    private PlacementDrive findDriveOrThrow(Long placementDriveId) {
        return placementDriveRepository.findById(placementDriveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected placement drive does not exist."));
    }

    private boolean isEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String normalized = trimmed.toLowerCase(Locale.ENGLISH);
        if ("-".equals(trimmed)
                || "n/a".equals(normalized)
                || "na".equals(normalized)
                || "null".equals(normalized)) {
            return null;
        }

        return trimmed;
    }

    private String prettifyLabel(String label) {
        return switch (label) {
            case "aptitude-material" -> "Aptitude Material PDF";
            case "coding-material" -> "Coding Material PDF";
            case "technical-topics" -> "Technical Topics PDF";
            case "hr-preparation" -> "HR Preparation PDF";
            default -> "PDF File";
        };
    }

    private String slugify(String value) {
        return value == null
                ? "resource"
                : value.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizePdfUrl(String storedUrl, String publicId) {
        if (storedUrl != null && (storedUrl.startsWith("https://") || storedUrl.startsWith("http://"))) {
            return appendPdfExtensionIfMissing(storedUrl);
        }
        if (publicId != null && !publicId.isBlank()) {
            return buildCloudinaryRawPdfUrl(publicId);
        }
        if (storedUrl != null && !storedUrl.isBlank()) {
            return buildCloudinaryRawPdfUrl(storedUrl);
        }
        return null;
    }

    private String buildCloudinaryRawPdfUrl(String publicId) {
        String cloudName = Objects.toString(cloudinary.config.cloudName, "").trim();
        if (cloudName.isBlank()) {
            return publicId;
        }
        return "https://res.cloudinary.com/" + cloudName + RAW_UPLOAD_PATH + ensurePdfSuffix(publicId);
    }

    private String appendPdfExtensionIfMissing(String url) {
        int queryStartIndex = url.indexOf('?');
        String baseUrl = queryStartIndex >= 0 ? url.substring(0, queryStartIndex) : url;
        String query = queryStartIndex >= 0 ? url.substring(queryStartIndex) : "";

        if (!baseUrl.contains(RAW_UPLOAD_PATH) || baseUrl.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
            return url;
        }

        return baseUrl + ".pdf" + query;
    }

    private String ensurePdfSuffix(String value) {
        return value.toLowerCase(Locale.ENGLISH).endsWith(".pdf") ? value : value + ".pdf";
    }

    private ResponseEntity<byte[]> getPdfResponse(Long id, String type, boolean download, boolean requireActive) {
        PreparationResource preparationResource = findResourceForPdfAccess(id, requireActive);
        PdfSelection pdfSelection = resolvePdfSelection(preparationResource, type);
        if (pdfSelection.url() == null || pdfSelection.url().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF not available");
        }

        byte[] pdfBytes = fetchPdfBytes(pdfSelection.url(), id, pdfSelection.type());
        String fileName = buildPdfFileName(preparationResource, pdfSelection.type());
        ContentDisposition contentDisposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdfBytes);
    }

    private PreparationResource findResourceForPdfAccess(Long id, boolean requireActive) {
        return preparationResourceRepository.findById(id)
                .filter(resource -> !requireActive || Boolean.TRUE.equals(resource.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preparation resource not found."));
    }

    private PreparationResourceStudentResponse toStudentResponse(PreparationResource preparationResource) {
        PlacementDrive placementDrive = preparationResource.getPlacementDrive();
        Company company = placementDrive.getCompany();

        return new PreparationResourceStudentResponse(
                preparationResource.getId(),
                placementDrive.getId(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                preparationResource.getResourceTitle(),
                preparationResource.getDescription(),
                hasPdf(preparationResource.getAptitudePdfUrl(), preparationResource.getAptitudePdfPublicId()),
                hasPdf(preparationResource.getCodingPdfUrl(), preparationResource.getCodingPdfPublicId()),
                hasPdf(preparationResource.getTechnicalPdfUrl(), preparationResource.getTechnicalPdfPublicId()),
                hasPdf(preparationResource.getHrPdfUrl(), preparationResource.getHrPdfPublicId())
        );
    }

    private PreparationResourceResponse toResponse(PreparationResource preparationResource) {
        PlacementDrive placementDrive = preparationResource.getPlacementDrive();
        Company company = placementDrive.getCompany();

        return new PreparationResourceResponse(
                preparationResource.getId(),
                placementDrive.getId(),
                placementDrive.getDriveTitle(),
                placementDrive.getHiringYear(),
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                preparationResource.getResourceTitle(),
                preparationResource.getDescription(),
                normalizePdfUrl(preparationResource.getAptitudePdfUrl(), preparationResource.getAptitudePdfPublicId()),
                normalizePdfUrl(preparationResource.getCodingPdfUrl(), preparationResource.getCodingPdfPublicId()),
                normalizePdfUrl(preparationResource.getTechnicalPdfUrl(), preparationResource.getTechnicalPdfPublicId()),
                normalizePdfUrl(preparationResource.getHrPdfUrl(), preparationResource.getHrPdfPublicId()),
                preparationResource.getActive(),
                preparationResource.getCreatedAt(),
                preparationResource.getUpdatedAt()
        );
    }

    private PdfSelection resolvePdfSelection(PreparationResource preparationResource, String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ENGLISH);

        return switch (normalizedType) {
            case "aptitude" -> new PdfSelection("aptitude-material",
                    normalizePdfUrl(preparationResource.getAptitudePdfUrl(), preparationResource.getAptitudePdfPublicId()));
            case "coding" -> new PdfSelection("coding-material",
                    normalizePdfUrl(preparationResource.getCodingPdfUrl(), preparationResource.getCodingPdfPublicId()));
            case "technical" -> new PdfSelection("technical-topics",
                    normalizePdfUrl(preparationResource.getTechnicalPdfUrl(), preparationResource.getTechnicalPdfPublicId()));
            case "hr" -> new PdfSelection("hr-preparation",
                    normalizePdfUrl(preparationResource.getHrPdfUrl(), preparationResource.getHrPdfPublicId()));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PDF type.");
        };
    }

    private byte[] fetchPdfBytes(String pdfUrl, Long resourceId, String type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pdfUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            LOGGER.warn("Cloudinary PDF fetch failed for resourceId={}, type={}, statusCode={}, url={}",
                    resourceId, type, response.statusCode(), pdfUrl);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch PDF file");
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid PDF URL configured for resourceId={}, type={}, url={}", resourceId, type, pdfUrl, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch PDF file");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("Cloudinary PDF fetch errored for resourceId={}, type={}, url={}", resourceId, type, pdfUrl, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch PDF file");
        }
    }

    private String buildPdfFileName(PreparationResource preparationResource, String type) {
        PlacementDrive placementDrive = preparationResource.getPlacementDrive();
        return String.join("-",
                nonBlankSlug(preparationResource.getPlacementDrive().getCompany().getCompanyName()),
                nonBlankSlug(placementDrive.getDriveTitle()),
                nonBlankSlug(type))
                + ".pdf";
    }

    private String nonBlankSlug(String value) {
        String slug = slugify(value);
        return slug.isBlank() ? "resource" : slug;
    }

    private boolean hasPdf(String url, String publicId) {
        return (url != null && !url.isBlank()) || (publicId != null && !publicId.isBlank());
    }

    private record UploadedFile(String secureUrl, String publicId) {
    }

    private record PdfSelection(String type, String url) {
    }
}

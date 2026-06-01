package com.iare.placementportal.service;

import com.iare.placementportal.dto.CompanyExcelUploadError;
import com.iare.placementportal.dto.CompanyExcelUploadResponse;
import com.iare.placementportal.dto.CompanyListDto;
import com.iare.placementportal.dto.CompanyPageResponse;
import com.iare.placementportal.dto.CompanyRequest;
import com.iare.placementportal.dto.CompanyResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.repository.CompanyRepository;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CompanyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyService.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final CompanyRepository companyRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public CompanyService(CompanyRepository companyRepository, PlatformTransactionManager transactionManager) {
        this.companyRepository = companyRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CompanyResponse createCompany(CompanyRequest request) {
        validateRequest(request);

        if (companyRepository.existsByCompanyNameIgnoreCase(request.companyName().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company with this name already exists.");
        }

        Company company = new Company();
        mapRequestToEntity(request, company);
        LOGGER.info("Company create requested: companyName='{}', incomingDescription='{}', finalDescription='{}', foundedYear={}",
                request.companyName(),
                request.description(),
                company.getDescription(),
                company.getFoundedYear());

        return toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getActiveCompaniesForStudents() {
        return companyRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyPageResponse getCompaniesPaginated(int page, int size, String search) {
        Pageable pageable = buildPageable(page, size);
        String normalizedSearch = normalizeSearch(search);
        long startTime = System.currentTimeMillis();
        Page<CompanyListDto> companyPage = companyRepository.findCompanyListPage(false, normalizedSearch, pageable);
        long elapsedTime = System.currentTimeMillis() - startTime;
        LOGGER.info("Student company page fetched in {} ms: page={}, size={}, search='{}', totalElements={}",
                elapsedTime, pageable.getPageNumber(), pageable.getPageSize(), normalizedSearch, companyPage.getTotalElements());
        return toPageResponse(companyPage, pageable);
    }

    @Transactional(readOnly = true)
    public CompanyPageResponse getAdminCompaniesPaginated(int page, int size, String search) {
        Pageable pageable = buildPageable(page, size);
        String normalizedSearch = normalizeSearch(search);
        long startTime = System.currentTimeMillis();
        Page<CompanyListDto> companyPage = companyRepository.findCompanyListPage(true, normalizedSearch, pageable);
        long elapsedTime = System.currentTimeMillis() - startTime;
        LOGGER.info("Admin company page fetched in {} ms: page={}, size={}, search='{}', totalElements={}",
                elapsedTime, pageable.getPageNumber(), pageable.getPageSize(), normalizedSearch, companyPage.getTotalElements());
        return toPageResponse(companyPage, pageable);
    }

    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        validateRequest(request);

        Company company = findCompanyOrThrow(id);
        companyRepository.findByCompanyNameIgnoreCase(request.companyName().trim())
                .filter(existingCompany -> !existingCompany.getId().equals(id))
                .ifPresent(existingCompany -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company with this name already exists.");
                });

        String existingDescriptionBefore = company.getDescription();
        Integer existingFoundedYearBefore = company.getFoundedYear();
        mapRequestToEntity(request, company);
        LOGGER.info("Company update requested: id={}, companyName='{}', incomingDescription='{}', existingDescriptionBefore='{}', finalDescription='{}', existingFoundedYearBefore={}, finalFoundedYear={}",
                id,
                request.companyName(),
                request.description(),
                existingDescriptionBefore,
                company.getDescription(),
                existingFoundedYearBefore,
                company.getFoundedYear());
        return toResponse(companyRepository.save(company));
    }

    public void deleteCompany(Long id) {
        Company company = findCompanyOrThrow(id);
        companyRepository.delete(company);
    }

    public CompanyResponse changeCompanyActiveStatus(Long id, boolean active) {
        Company company = findCompanyOrThrow(id);
        company.setActive(active);
        return toResponse(companyRepository.save(company));
    }

    public CompanyExcelUploadResponse uploadCompaniesFromExcel(MultipartFile file) {
        validateExcelFile(file);
        LOGGER.info("Company Excel upload started: fileName='{}', size={} bytes",
                file.getOriginalFilename(), file.getSize());

        List<CompanyExcelUploadError> errors = new ArrayList<>();
        int totalRows = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file does not contain any sheet.");
            }

            int headerRowIndex = detectHeaderRowIndex(sheet);
            if (headerRowIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unable to detect the header row. Ensure the sheet contains a Company Name column.");
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(headerRow);
            LOGGER.info("Company upload header row {} raw headers: {}", headerRowIndex + 1, extractRawHeaders(headerRow));
            if (findHeaderIndex(headerIndexMap, "Company Name") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header not mapped: Company Name");
            }
            if (findFirstHeaderIndex(headerIndexMap, "Company Description", "Description", "About Company") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Description header not found. Expected one of: Company Description, Description, About Company.");
            }
            if (findFirstHeaderIndex(headerIndexMap, "Founded Year", "Founded", "FoundedYear", "Year Founded") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Founded year header not found. Expected one of: Founded Year, Founded, FoundedYear, Year Founded.");
            }

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }

                totalRows++;

                try {
                    CompanyRowData rowData = readCompanyRow(row, headerIndexMap);
                    boolean updatedExistingCompany = persistCompanyRow(rowData);
                    if (updatedExistingCompany) {
                        updatedCount++;
                    } else {
                        insertedCount++;
                    }
                } catch (RowValidationException exception) {
                    skippedCount++;
                    errors.add(new CompanyExcelUploadError(rowIndex + 1, exception.getMessage()));
                } catch (RuntimeException exception) {
                    skippedCount++;
                    String reason = buildRowErrorMessage(exception);
                    errors.add(new CompanyExcelUploadError(rowIndex + 1, reason));
                    LOGGER.error("Failed to process company upload row {}: {}", rowIndex + 1, reason, exception);
                }
            }
        } catch (EncryptedDocumentException exception) {
            LOGGER.warn("Uploaded company Excel file is invalid or unsupported.", exception);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unable to read this Excel file. Please upload a valid .xlsx or .xls company sheet.");
        } catch (IOException exception) {
            LOGGER.error("Failed to read uploaded company Excel file.", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read uploaded Excel file.");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.error("Unexpected failure while uploading company Excel file.", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to process uploaded company Excel file. Please verify the sheet format and try again.");
        }

        LOGGER.info("Company Excel upload completed: totalRows={}, insertedCount={}, updatedCount={}, skippedCount={}, errorCount={}",
                totalRows, insertedCount, updatedCount, skippedCount, errors.size());

        return new CompanyExcelUploadResponse(totalRows, insertedCount, updatedCount, skippedCount, errors);
    }

    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found."));
    }

    private void validateRequest(CompanyRequest request) {
        if (request.foundedYear() != null && request.foundedYear() > Year.now().getValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Founded Year cannot be in the future.");
        }

        if (normalizeOptional(request.description()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company Description is required.");
        }

        validateUrl(request.logoUrl(), "Company Logo URL must be a valid URL.");
        validateUrl(request.websiteUrl(), "Company Website URL must be a valid URL.");
    }

    private void mapRequestToEntity(CompanyRequest request, Company company) {
        company.setCompanyName(request.companyName().trim());
        company.setLogoUrl(normalizeOptional(request.logoUrl()));
        company.setWebsiteUrl(normalizeOptional(request.websiteUrl()));
        company.setCompanyType(request.companyType().trim());
        company.setIndustry(request.industry().trim());
        company.setHeadquarters(normalizeOptional(request.headquarters()));
        company.setFoundedYear(request.foundedYear());
        company.setDescription(normalizeOptional(request.description()));
        if (company.getActive() == null) {
            company.setActive(true);
        }
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

    private void validateUrl(String value, String message) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            return;
        }

        try {
            URI uri = new URI(normalizedValue);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getCompanyType(),
                company.getIndustry(),
                company.getHeadquarters(),
                company.getFoundedYear(),
                company.getDescription(),
                company.getActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    private CompanyPageResponse toPageResponse(Page<CompanyListDto> companyPage, Pageable pageable) {
        return new CompanyPageResponse(
                companyPage.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isFirst(),
                companyPage.isLast()
        );
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim();
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file is required.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ENGLISH);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx and .xls Excel files are supported.");
        }
    }

    private int detectHeaderRowIndex(Sheet sheet) {
        int lastRowIndexToCheck = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 9);
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= lastRowIndexToCheck; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            for (Cell cell : row) {
                String normalizedValue = normalizeHeader(DATA_FORMATTER.formatCellValue(cell));
                if ("companyname".equals(normalizedValue)) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalizedHeader = normalizeHeader(DATA_FORMATTER.formatCellValue(cell));
            if (!normalizedHeader.isBlank()) {
                headerIndexMap.put(normalizedHeader, cell.getColumnIndex());
            }
        }
        return headerIndexMap;
    }

    private CompanyRowData readCompanyRow(Row row, Map<String, Integer> headerIndexMap) {
        String companyName = normalizeOptional(readStringByHeaders(row, headerIndexMap, "Company Name"));
        if (companyName == null) {
            throw new RowValidationException("Company Name is required.");
        }

        String logoUrl = normalizeOptional(readStringByHeaders(row, headerIndexMap, "Company Logo URL"));
        String websiteUrl = normalizeOptional(readStringByHeaders(row, headerIndexMap, "Company Website URL"));
        String companyType = defaultString(normalizeOptional(readStringByHeaders(row, headerIndexMap, "Company Type")));
        String industry = defaultString(normalizeOptional(readStringByHeaders(row, headerIndexMap, "Industry")));
        String headquarters = normalizeOptional(readStringByHeaders(row, headerIndexMap, "Headquarters"));
        ParsedFoundedYear foundedYearData = parseFoundedYear(row, headerIndexMap);
        String rawDescription = readStringByHeaders(
                row,
                headerIndexMap,
                "Company Description",
                "Description",
                "About Company"
        );
        String description = normalizeOptional(rawDescription);

        validateImportedValues(companyName, foundedYearData.value());
        LOGGER.info("Excel company row read: companyName='{}', rawFoundedYear='{}', parsedFoundedYear={}, rawDescription='{}', parsedDescription='{}'",
                companyName,
                foundedYearData.rawValue(),
                foundedYearData.value(),
                rawDescription,
                description);

        return new CompanyRowData(
                companyName,
                logoUrl,
                websiteUrl,
                companyType,
                industry,
                headquarters,
                foundedYearData.value(),
                description
        );
    }

    private void validateImportedValues(String companyName, Integer foundedYear) {
        if (companyName == null) {
            throw new RowValidationException("Company Name is required.");
        }
        if (foundedYear != null && foundedYear > Year.now().getValue()) {
            throw new RowValidationException("Founded Year cannot be in the future.");
        }
    }

    private boolean persistCompanyRow(CompanyRowData rowData) {
        Boolean existing = requiresNewTransactionTemplate.execute(status -> {
            Optional<Company> existingCompanyOptional = findExistingCompanyForUpload(rowData.companyName());
            Company company = existingCompanyOptional.orElseGet(Company::new);
            boolean companyExists = company.getId() != null;

            if (companyExists) {
                String existingDescriptionBefore = company.getDescription();
                Integer existingFoundedYearBefore = company.getFoundedYear();
                LOGGER.info("Existing company found for upload: excelName='{}', matchedDbName='{}'",
                        rowData.companyName(), company.getCompanyName());
                if (rowData.foundedYear() != null) {
                    company.setFoundedYear(rowData.foundedYear());
                }
                if (rowData.description() != null) {
                    company.setDescription(rowData.description());
                }
                company.setActive(true);
                LOGGER.info("Updating company: {}, excelDescription={}, excelFoundedYear={}, existingDescriptionBefore={}, existingFoundedYearBefore={}",
                        company.getCompanyName(),
                        rowData.description(),
                        rowData.foundedYear(),
                        existingDescriptionBefore,
                        existingFoundedYearBefore);
            } else {
                LOGGER.info("Creating new company from upload: {}", rowData.companyName());
                validateUrl(rowData.logoUrl(), "Company Logo URL must be a valid URL.");
                validateUrl(rowData.websiteUrl(), "Company Website URL must be a valid URL.");

                company.setCompanyName(rowData.companyName());
                company.setLogoUrl(rowData.logoUrl());
                company.setWebsiteUrl(rowData.websiteUrl());
                company.setCompanyType(rowData.companyType());
                company.setIndustry(rowData.industry());
                company.setHeadquarters(rowData.headquarters());
                company.setFoundedYear(rowData.foundedYear());
                company.setDescription(defaultString(rowData.description()));
                company.setActive(true);
            }

            Company savedCompany = companyRepository.saveAndFlush(company);
            Company reloadedCompany = companyRepository.findById(savedCompany.getId()).orElse(savedCompany);
            LOGGER.info("Company upload save successful: id={}, companyName='{}', foundedYear={}, description={}, reloadedFoundedYear={}, reloadedDescription={}",
                    savedCompany.getId(),
                    savedCompany.getCompanyName(),
                    savedCompany.getFoundedYear(),
                    savedCompany.getDescription(),
                    reloadedCompany.getFoundedYear(),
                    reloadedCompany.getDescription());
            return companyExists;
        });

        return Boolean.TRUE.equals(existing);
    }

    private Optional<Company> findExistingCompanyForUpload(String companyName) {
        String normalizedName = normalizeOptional(companyName);
        if (normalizedName == null) {
            return Optional.empty();
        }

        Optional<Company> exactMatch = companyRepository.findByCompanyNameIgnoreCase(normalizedName);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String canonicalInput = canonicalizeCompanyName(normalizedName);
        List<Company> companies = companyRepository.findAll();

        List<Company> canonicalMatches = companies.stream()
                .filter(company -> canonicalizeCompanyName(company.getCompanyName()).equals(canonicalInput))
                .toList();
        if (canonicalMatches.size() == 1) {
            return Optional.of(canonicalMatches.get(0));
        }

        List<Company> aliasMatches = companies.stream()
                .filter(company -> {
                    String canonicalCompanyName = canonicalizeCompanyName(company.getCompanyName());
                    return canonicalCompanyName.contains(canonicalInput) || canonicalInput.contains(canonicalCompanyName);
                })
                .toList();
        if (aliasMatches.size() == 1) {
            return Optional.of(aliasMatches.get(0));
        }

        return Optional.empty();
    }

    private ParsedFoundedYear parseFoundedYear(Row row, Map<String, Integer> headerIndexMap) {
        Integer cellIndex = findFirstHeaderIndex(
                headerIndexMap,
                "Founded Year",
                "Founded",
                "FoundedYear",
                "Year Founded"
        );
        if (cellIndex == null) {
            throw new RowValidationException("Founded year header not found.");
        }

        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return new ParsedFoundedYear("", null);
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (numericValue % 1 != 0) {
                throw new RowValidationException("Founded Year must be a valid whole number year.");
            }
            return new ParsedFoundedYear(DATA_FORMATTER.formatCellValue(cell).trim(), (int) numericValue);
        }

        String rawCellValue = readCellAsString(row, cellIndex);
        String rawValue = normalizeOptional(rawCellValue);
        if (rawValue == null) {
            return new ParsedFoundedYear(rawCellValue, null);
        }

        String digitsOnly = rawValue.replaceAll("[^0-9.]", "");
        if (digitsOnly.isEmpty()) {
            throw new RowValidationException("Founded year cell could not be parsed: " + rawValue);
        }

        try {
            if (digitsOnly.contains(".")) {
                return new ParsedFoundedYear(rawValue, (int) Double.parseDouble(digitsOnly));
            }
            return new ParsedFoundedYear(rawValue, Integer.parseInt(digitsOnly));
        } catch (NumberFormatException exception) {
            throw new RowValidationException("Founded year cell could not be parsed: " + rawValue);
        }
    }

    private String readString(Row row, Map<String, Integer> headerIndexMap, String headerName) {
        Integer cellIndex = findHeaderIndex(headerIndexMap, headerName);
        if (cellIndex == null) {
            return "";
        }
        return readCellAsString(row, cellIndex);
    }

    private String readStringByHeaders(Row row, Map<String, Integer> headerIndexMap, String... headerNames) {
        Integer cellIndex = findFirstHeaderIndex(headerIndexMap, headerNames);
        if (cellIndex == null) {
            return "";
        }
        return readCellAsString(row, cellIndex);
    }

    private Integer findHeaderIndex(Map<String, Integer> headerIndexMap, String headerName) {
        return headerIndexMap.get(normalizeHeader(headerName));
    }

    private Integer findFirstHeaderIndex(Map<String, Integer> headerIndexMap, String... headerNames) {
        for (String headerName : headerNames) {
            Integer cellIndex = findHeaderIndex(headerIndexMap, headerName);
            if (cellIndex != null) {
                return cellIndex;
            }
        }

        for (Map.Entry<String, Integer> entry : headerIndexMap.entrySet()) {
            String normalizedHeader = entry.getKey();
            for (String headerName : headerNames) {
                String normalizedTarget = normalizeHeader(headerName);
                if (normalizedHeader.contains(normalizedTarget) || normalizedTarget.contains(normalizedHeader)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String readCellAsString(Row row, Integer index) {
        if (row == null || index == null || index < 0) {
            return "";
        }

        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }

        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !DATA_FORMATTER.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH)
                .replace(".", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
    }

    private List<String> extractRawHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        if (headerRow == null) {
            return headers;
        }

        for (Cell cell : headerRow) {
            headers.add(DATA_FORMATTER.formatCellValue(cell));
        }
        return headers;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String canonicalizeCompanyName(String value) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            return "";
        }

        return normalizedValue.toLowerCase(Locale.ENGLISH)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]", "");
    }

    private String buildRowErrorMessage(RuntimeException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        if (exception instanceof DataAccessException || cause instanceof DataAccessException) {
            return "Database save failed: " + sanitizeExceptionMessage(cause.getMessage());
        }

        String message = sanitizeExceptionMessage(exception.getMessage());
        if (message != null) {
            return message;
        }

        String causeMessage = sanitizeExceptionMessage(cause.getMessage());
        return causeMessage != null ? causeMessage : "Unable to process company record";
    }

    private String sanitizeExceptionMessage(String message) {
        if (message == null) {
            return null;
        }
        String singleLine = message.replaceAll("\\s+", " ").trim();
        return singleLine.isEmpty() ? null : singleLine;
    }

    private record CompanyRowData(
            String companyName,
            String logoUrl,
            String websiteUrl,
            String companyType,
            String industry,
            String headquarters,
            Integer foundedYear,
            String description
    ) {
    }

    private record ParsedFoundedYear(
            String rawValue,
            Integer value
    ) {
    }

    private static class RowValidationException extends RuntimeException {
        private RowValidationException(String message) {
            super(message);
        }
    }
}

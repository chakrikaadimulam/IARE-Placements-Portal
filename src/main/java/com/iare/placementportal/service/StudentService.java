package com.iare.placementportal.service;

import com.iare.placementportal.dto.StudentExcelUploadResponse;
import com.iare.placementportal.dto.StudentLoginRequest;
import com.iare.placementportal.dto.StudentLoginResponse;
import com.iare.placementportal.dto.StudentResponse;
import com.iare.placementportal.entity.Student;
import com.iare.placementportal.repository.StudentRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter PASSWORD_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final String PHOTO_URL_TEMPLATE =
            "https://iare-data.s3.ap-south-1.amazonaws.com/uploads/STUDENTS/%s/%s.jpg";

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public StudentExcelUploadResponse uploadStudentsFromExcel(MultipartFile file) {
        validateExcelFile(file);

        List<String> errors = new ArrayList<>();
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
                        "Unable to detect Excel header row. Ensure the sheet contains Roll No and Student Name headers.");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(sheet.getRow(headerRowIndex));

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }

                totalRows++;

                try {
                    StudentRowData rowData = readStudentRow(row, headerIndexMap);
                    Student student = studentRepository.findByRollNoIgnoreCase(rowData.rollNo())
                            .orElseGet(Student::new);
                    boolean existing = student.getId() != null;

                    mapStudent(student, rowData);
                    studentRepository.save(student);

                    if (existing) {
                        updatedCount++;
                    } else {
                        insertedCount++;
                    }
                } catch (RowValidationException exception) {
                    skippedCount++;
                    errors.add("Row " + (rowIndex + 1) + ": " + exception.getMessage());
                } catch (RuntimeException exception) {
                    skippedCount++;
                    errors.add("Row " + (rowIndex + 1) + ": Unable to process student record.");
                    LOGGER.warn("Failed to process student Excel row {}.", rowIndex + 1, exception);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to read uploaded student Excel file.", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read uploaded Excel file.");
        }

        return new StudentExcelUploadResponse(totalRows, insertedCount, updatedCount, skippedCount, errors);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getActiveStudents() {
        return studentRepository.findByActiveTrueOrderByStudentNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return toResponse(findStudentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByRollNo(String rollNo) {
        return toResponse(findByRollNoOrThrow(rollNo));
    }

    @Transactional(readOnly = true)
    public StudentLoginResponse studentLogin(StudentLoginRequest request) {
        if (request == null || isBlank(request.rollNo()) || isBlank(request.password())) {
            return new StudentLoginResponse(false, "Roll No and password are required.", null, null, null, null, null, null, null);
        }

        Optional<Student> studentOptional = studentRepository.findByRollNoIgnoreCase(request.rollNo().trim());
        if (studentOptional.isEmpty()) {
            if ("student".equalsIgnoreCase(request.rollNo().trim()) && "student123".equals(request.password().trim())) {
                return new StudentLoginResponse(true, "Demo student login successful.", 0L, "student", "Demo Student", "CSE", 4, "A", null);
            }
            return new StudentLoginResponse(false, "Invalid roll number or password.", null, null, null, null, null, null, null);
        }

        Student student = studentOptional.get();
        if (!Boolean.TRUE.equals(student.getActive())) {
            return new StudentLoginResponse(false, "Student account is currently inactive.", null, null, null, null, null, null, null);
        }
        if (!Objects.equals(student.getPassword(), request.password().trim())) {
            return new StudentLoginResponse(false, "Invalid roll number or password.", null, null, null, null, null, null, null);
        }

        return new StudentLoginResponse(
                true,
                "Student login successful.",
                student.getId(),
                student.getRollNo(),
                student.getStudentName(),
                student.getBranch(),
                student.getSemester(),
                student.getSection(),
                student.getPhotoUrl()
        );
    }

    public StudentResponse changeStudentActiveStatus(Long id, boolean active) {
        Student student = findStudentOrThrow(id);
        student.setActive(active);
        return toResponse(studentRepository.save(student));
    }

    public void deleteStudent(Long id) {
        studentRepository.delete(findStudentOrThrow(id));
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
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            boolean hasRollNo = false;
            boolean hasStudentName = false;
            for (Cell cell : row) {
                String normalizedValue = normalizeHeader(DATA_FORMATTER.formatCellValue(cell));
                if ("roll no".equals(normalizedValue)) {
                    hasRollNo = true;
                }
                if ("student name".equals(normalizedValue)) {
                    hasStudentName = true;
                }
            }

            if (hasRollNo && hasStudentName) {
                return rowIndex;
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

    private StudentRowData readStudentRow(Row row, Map<String, Integer> headerIndexMap) {
        String rollNo = normalizeOptional(readString(row, headerIndexMap, "roll no"));
        if (isBlank(rollNo)) {
            throw new RowValidationException("Roll No is required.");
        }

        String studentName = normalizeOptional(readString(row, headerIndexMap, "student name"));
        if (isBlank(studentName)) {
            throw new RowValidationException("Student Name is required.");
        }

        ParsedDate dob = parseDateCell(row, headerIndexMap, "dob");
        if (dob == null) {
            throw new RowValidationException("DOB is required and must be a valid date.");
        }

        ParsedDate doj = parseDateCell(row, headerIndexMap, "doj");

        return new StudentRowData(
                rollNo.toUpperCase(Locale.ENGLISH),
                dob.passwordValue(),
                studentName,
                normalizeOptional(readString(row, headerIndexMap, "gender")),
                normalizeOptional(readString(row, headerIndexMap, "status")),
                normalizeOptional(readString(row, headerIndexMap, "cast")),
                normalizeOptional(readString(row, headerIndexMap, "sub cast")),
                normalizeOptional(readString(row, headerIndexMap, "religion")),
                normalizeOptional(readString(row, headerIndexMap, "branch")),
                parseInteger(readString(row, headerIndexMap, "semester")),
                normalizeOptional(readString(row, headerIndexMap, "admission category")),
                normalizeOptional(readString(row, headerIndexMap, "fee category")),
                normalizeOptional(readString(row, headerIndexMap, "cet rank")),
                normalizeOptional(readString(row, headerIndexMap, "ssc marks")),
                normalizeOptional(readString(row, headerIndexMap, "ssc")),
                normalizeOptional(readString(row, headerIndexMap, "inter marks")),
                normalizeOptional(readString(row, headerIndexMap, "inter")),
                normalizeOptional(readString(row, headerIndexMap, "ug marks")),
                normalizeOptional(readString(row, headerIndexMap, "ug")),
                dob.displayValue(),
                doj == null ? null : doj.displayValue(),
                normalizeOptional(readString(row, headerIndexMap, "father name")),
                normalizeOptional(readString(row, headerIndexMap, "mother name")),
                normalizeOptional(readString(row, headerIndexMap, "student phone")),
                firstNonBlank(
                        normalizeOptional(readString(row, headerIndexMap, "parent phone")),
                        normalizeOptional(readString(row, headerIndexMap, "father phone"))
                ),
                normalizeOptional(readString(row, headerIndexMap, "mother phone")),
                normalizeOptional(readString(row, headerIndexMap, "student email id")),
                normalizeOptional(readString(row, headerIndexMap, "current address")),
                normalizeOptional(readString(row, headerIndexMap, "permanent address")),
                normalizeOptional(readString(row, headerIndexMap, "aadhar")),
                normalizeOptional(readString(row, headerIndexMap, "father occupation")),
                normalizeOptional(readString(row, headerIndexMap, "occupation type")),
                normalizeOptional(readString(row, headerIndexMap, "income")),
                normalizeOptional(readString(row, headerIndexMap, "section")),
                normalizeOptional(readString(row, headerIndexMap, "moles")),
                normalizeOptional(readString(row, headerIndexMap, "place of birth")),
                normalizeOptional(readString(row, headerIndexMap, "current dno")),
                normalizeOptional(readString(row, headerIndexMap, "current street")),
                normalizeOptional(readString(row, headerIndexMap, "current village town")),
                normalizeOptional(readString(row, headerIndexMap, "current mandal")),
                normalizeOptional(readString(row, headerIndexMap, "current district")),
                normalizeOptional(readString(row, headerIndexMap, "current state")),
                normalizeOptional(readString(row, headerIndexMap, "current pincode")),
                normalizeOptional(readString(row, headerIndexMap, "permanent dno")),
                normalizeOptional(readString(row, headerIndexMap, "permanent street")),
                normalizeOptional(readString(row, headerIndexMap, "permanent village town")),
                normalizeOptional(readString(row, headerIndexMap, "permanent mandal")),
                normalizeOptional(readString(row, headerIndexMap, "permanent district")),
                normalizeOptional(readString(row, headerIndexMap, "permanent state")),
                normalizeOptional(readString(row, headerIndexMap, "permanent pincode")),
                normalizeOptional(readString(row, headerIndexMap, "domicile state")),
                normalizeOptional(readString(row, headerIndexMap, "ssc state")),
                normalizeOptional(readString(row, headerIndexMap, "inter state"))
        );
    }

    private void mapStudent(Student student, StudentRowData rowData) {
        student.setRollNo(rowData.rollNo());
        student.setPassword(rowData.password());
        student.setStudentName(rowData.studentName());
        student.setGender(rowData.gender());
        student.setStatus(rowData.status());
        student.setCaste(rowData.caste());
        student.setSubCaste(rowData.subCaste());
        student.setReligion(rowData.religion());
        student.setBranch(rowData.branch());
        student.setSemester(rowData.semester());
        student.setAdmissionCategory(rowData.admissionCategory());
        student.setFeeCategory(rowData.feeCategory());
        student.setCetRank(rowData.cetRank());
        student.setSscMarks(rowData.sscMarks());
        student.setSscPercentage(rowData.sscPercentage());
        student.setInterMarks(rowData.interMarks());
        student.setInterPercentage(rowData.interPercentage());
        student.setUgMarks(rowData.ugMarks());
        student.setUgPercentage(rowData.ugPercentage());
        student.setDob(rowData.dob());
        student.setDoj(rowData.doj());
        student.setFatherName(rowData.fatherName());
        student.setMotherName(rowData.motherName());
        student.setStudentPhone(rowData.studentPhone());
        student.setParentPhone(rowData.parentPhone());
        student.setMotherPhone(rowData.motherPhone());
        student.setStudentEmailId(rowData.studentEmailId());
        student.setCurrentAddress(rowData.currentAddress());
        student.setPermanentAddress(rowData.permanentAddress());
        student.setAadhar(rowData.aadhar());
        student.setFatherOccupation(rowData.fatherOccupation());
        student.setOccupationType(rowData.occupationType());
        student.setIncome(rowData.income());
        student.setSection(rowData.section());
        student.setMoles(rowData.moles());
        student.setPlaceOfBirth(rowData.placeOfBirth());
        student.setCurrentDno(rowData.currentDno());
        student.setCurrentStreet(rowData.currentStreet());
        student.setCurrentVillageTown(rowData.currentVillageTown());
        student.setCurrentMandal(rowData.currentMandal());
        student.setCurrentDistrict(rowData.currentDistrict());
        student.setCurrentState(rowData.currentState());
        student.setCurrentPincode(rowData.currentPincode());
        student.setPermanentDno(rowData.permanentDno());
        student.setPermanentStreet(rowData.permanentStreet());
        student.setPermanentVillageTown(rowData.permanentVillageTown());
        student.setPermanentMandal(rowData.permanentMandal());
        student.setPermanentDistrict(rowData.permanentDistrict());
        student.setPermanentState(rowData.permanentState());
        student.setPermanentPincode(rowData.permanentPincode());
        student.setDomicileState(rowData.domicileState());
        student.setSscState(rowData.sscState());
        student.setInterState(rowData.interState());
        student.setPhotoUrl(buildPhotoUrl(rowData.rollNo()));
        if (student.getActive() == null) {
            student.setActive(true);
        }
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getRollNo(),
                student.getStudentName(),
                student.getGender(),
                student.getStatus(),
                student.getCaste(),
                student.getSubCaste(),
                student.getReligion(),
                student.getBranch(),
                student.getSemester(),
                student.getAdmissionCategory(),
                student.getFeeCategory(),
                student.getCetRank(),
                student.getSscMarks(),
                student.getSscPercentage(),
                student.getInterMarks(),
                student.getInterPercentage(),
                student.getUgMarks(),
                student.getUgPercentage(),
                student.getDob(),
                student.getDoj(),
                student.getFatherName(),
                student.getMotherName(),
                student.getStudentPhone(),
                student.getParentPhone(),
                student.getMotherPhone(),
                student.getStudentEmailId(),
                student.getCurrentAddress(),
                student.getPermanentAddress(),
                student.getAadhar(),
                student.getFatherOccupation(),
                student.getOccupationType(),
                student.getIncome(),
                student.getSection(),
                student.getMoles(),
                student.getPlaceOfBirth(),
                student.getCurrentDno(),
                student.getCurrentStreet(),
                student.getCurrentVillageTown(),
                student.getCurrentMandal(),
                student.getCurrentDistrict(),
                student.getCurrentState(),
                student.getCurrentPincode(),
                student.getPermanentDno(),
                student.getPermanentStreet(),
                student.getPermanentVillageTown(),
                student.getPermanentMandal(),
                student.getPermanentDistrict(),
                student.getPermanentState(),
                student.getPermanentPincode(),
                student.getDomicileState(),
                student.getSscState(),
                student.getInterState(),
                student.getPhotoUrl(),
                student.getActive(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
    }

    private Student findByRollNoOrThrow(String rollNo) {
        return studentRepository.findByRollNoIgnoreCase(rollNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !DATA_FORMATTER.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String readString(Row row, Map<String, Integer> headerIndexMap, String headerAlias) {
        Integer cellIndex = findHeaderIndex(headerIndexMap, headerAlias);
        if (cellIndex == null) {
            return null;
        }
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private Integer findHeaderIndex(Map<String, Integer> headerIndexMap, String headerAlias) {
        return headerIndexMap.get(normalizeHeader(headerAlias));
    }

    private ParsedDate parseDateCell(Row row, Map<String, Integer> headerIndexMap, String headerAlias) {
        Integer cellIndex = findHeaderIndex(headerIndexMap, headerAlias);
        if (cellIndex == null) {
            return null;
        }

        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDate localDate = Instant.ofEpochMilli(cell.getDateCellValue().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return new ParsedDate(localDate.format(DISPLAY_DATE_FORMAT), localDate.format(PASSWORD_DATE_FORMAT));
        }

        String cellValue = DATA_FORMATTER.formatCellValue(cell).trim();
        if (cellValue.isEmpty()) {
            return null;
        }

        String normalized = cellValue.replace('.', '-').replace('/', '-');
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd-M-yyyy"),
                DateTimeFormatter.ofPattern("d-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH)
        )) {
            try {
                LocalDate parsedDate = LocalDate.parse(normalized, formatter);
                return new ParsedDate(parsedDate.format(DISPLAY_DATE_FORMAT), parsedDate.format(PASSWORD_DATE_FORMAT));
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private Integer parseInteger(String value) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            return null;
        }

        try {
            if (normalizedValue.contains(".")) {
                return (int) Double.parseDouble(normalizedValue);
            }
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildPhotoUrl(String rollNo) {
        return String.format(PHOTO_URL_TEMPLATE, rollNo, rollNo);
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private record ParsedDate(String displayValue, String passwordValue) {
    }

    private record StudentRowData(
            String rollNo,
            String password,
            String studentName,
            String gender,
            String status,
            String caste,
            String subCaste,
            String religion,
            String branch,
            Integer semester,
            String admissionCategory,
            String feeCategory,
            String cetRank,
            String sscMarks,
            String sscPercentage,
            String interMarks,
            String interPercentage,
            String ugMarks,
            String ugPercentage,
            String dob,
            String doj,
            String fatherName,
            String motherName,
            String studentPhone,
            String parentPhone,
            String motherPhone,
            String studentEmailId,
            String currentAddress,
            String permanentAddress,
            String aadhar,
            String fatherOccupation,
            String occupationType,
            String income,
            String section,
            String moles,
            String placeOfBirth,
            String currentDno,
            String currentStreet,
            String currentVillageTown,
            String currentMandal,
            String currentDistrict,
            String currentState,
            String currentPincode,
            String permanentDno,
            String permanentStreet,
            String permanentVillageTown,
            String permanentMandal,
            String permanentDistrict,
            String permanentState,
            String permanentPincode,
            String domicileState,
            String sscState,
            String interState
    ) {
    }

    private static class RowValidationException extends RuntimeException {
        private RowValidationException(String message) {
            super(message);
        }
    }
}

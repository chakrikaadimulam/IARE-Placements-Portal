package com.iare.placementportal.service;

import com.iare.placementportal.dto.CompanyExcelUploadResponse;
import com.iare.placementportal.entity.Company;
import com.iare.placementportal.repository.CompanyRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CompanyServiceExcelUploadTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        companyRepository.deleteAll();
    }

    @Test
    void uploadCompaniesFromExcel_updatesExistingCompanyDescriptionAndFoundedYear() throws Exception {
        Company existingCompany = new Company();
        existingCompany.setCompanyName("Microsoft");
        existingCompany.setLogoUrl("https://existing.example/logo.png");
        existingCompany.setWebsiteUrl("https://www.microsoft.com");
        existingCompany.setCompanyType("Product");
        existingCompany.setIndustry("Software");
        existingCompany.setHeadquarters("Redmond, WA");
        existingCompany.setFoundedYear(null);
        existingCompany.setDescription("");
        existingCompany.setActive(true);
        companyRepository.saveAndFlush(existingCompany);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "companies.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildCompanyWorkbook()
        );

        CompanyExcelUploadResponse response = companyService.uploadCompaniesFromExcel(file);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.insertedCount());
        assertEquals(1, response.updatedCount());
        assertEquals(0, response.skippedCount());

        Company updatedCompany = companyRepository.findByCompanyNameIgnoreCase("Microsoft").orElseThrow();
        assertEquals(Integer.valueOf(1975), updatedCompany.getFoundedYear());
        assertEquals("Microsoft builds software, cloud, and AI platforms.", updatedCompany.getDescription());
        assertEquals("https://existing.example/logo.png", updatedCompany.getLogoUrl());
    }

    @Test
    void uploadCompaniesFromExcel_matchesAliasNamesAndUpdatesExistingCompany() throws Exception {
        Company existingCompany = new Company();
        existingCompany.setCompanyName("TCS");
        existingCompany.setLogoUrl("https://existing.example/tcs-logo.png");
        existingCompany.setWebsiteUrl("https://www.tcs.com");
        existingCompany.setCompanyType("Service");
        existingCompany.setIndustry("IT Services");
        existingCompany.setHeadquarters("Mumbai");
        existingCompany.setFoundedYear(null);
        existingCompany.setDescription("");
        existingCompany.setActive(true);
        companyRepository.saveAndFlush(existingCompany);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "companies.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildAliasWorkbook()
        );

        CompanyExcelUploadResponse response = companyService.uploadCompaniesFromExcel(file);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.insertedCount());
        assertEquals(1, response.updatedCount());
        assertEquals(0, response.skippedCount());

        Company updatedCompany = companyRepository.findByCompanyNameIgnoreCase("TCS").orElseThrow();
        assertEquals(Integer.valueOf(1968), updatedCompany.getFoundedYear());
        assertEquals("TCS provides consulting and software services globally.", updatedCompany.getDescription());
        assertEquals("https://existing.example/tcs-logo.png", updatedCompany.getLogoUrl());
        assertEquals(1, companyRepository.count());
    }

    private byte[] buildCompanyWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Companies").createRow(0);
            header.createCell(0).setCellValue("S.No");
            header.createCell(1).setCellValue("Company Name");
            header.createCell(2).setCellValue("Company Logo URL");
            header.createCell(3).setCellValue("Company Website URL");
            header.createCell(4).setCellValue("Company Type");
            header.createCell(5).setCellValue("Industry");
            header.createCell(6).setCellValue("Headquarters");
            header.createCell(7).setCellValue("Founded Year");
            header.createCell(8).setCellValue("Company Description");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("Microsoft");
            row.createCell(2).setCellValue("https://new.example/logo.png");
            row.createCell(3).setCellValue("https://www.microsoft.com");
            row.createCell(4).setCellValue("Product");
            row.createCell(5).setCellValue("Software");
            row.createCell(6).setCellValue("Redmond, WA");
            row.createCell(7).setCellValue(1975);
            row.createCell(8).setCellValue("Microsoft builds software, cloud, and AI platforms.");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildAliasWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Companies").createRow(0);
            header.createCell(0).setCellValue("S.No");
            header.createCell(1).setCellValue("Company Name");
            header.createCell(2).setCellValue("Company Logo URL");
            header.createCell(3).setCellValue("Company Website URL");
            header.createCell(4).setCellValue("Company Type");
            header.createCell(5).setCellValue("Industry");
            header.createCell(6).setCellValue("Headquarters");
            header.createCell(7).setCellValue("Founded Year");
            header.createCell(8).setCellValue("Company Description");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("Tata Consultancy Services (TCS)");
            row.createCell(2).setCellValue("https://new.example/tcs-logo.png");
            row.createCell(3).setCellValue("https://www.tcs.com");
            row.createCell(4).setCellValue("Service");
            row.createCell(5).setCellValue("IT Services");
            row.createCell(6).setCellValue("Mumbai");
            row.createCell(7).setCellValue(1968);
            row.createCell(8).setCellValue("TCS provides consulting and software services globally.");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}

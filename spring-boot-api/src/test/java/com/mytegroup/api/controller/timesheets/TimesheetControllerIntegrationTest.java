package com.mytegroup.api.controller.timesheets;

import com.mytegroup.api.dto.timesheets.CreateTimesheetDto;
import com.mytegroup.api.dto.timesheets.TimesheetEntryDto;
import com.mytegroup.api.dto.timesheets.UpdateTimesheetDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.timesheets.Timesheet;
import com.mytegroup.api.repository.timesheets.TimesheetRepository;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TimesheetControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Office testOffice;
    private Project testProject;
    private Person testPerson;

    @Autowired
    private TimesheetRepository timesheetRepository;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testOrganization = setupOrganization("Test Organization");
        testOffice = setupOffice(testOrganization);
        testProject = setupProject(testOrganization, testOffice);
        testPerson = setupPerson(testOrganization);
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTimesheets_WithOrgAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/timesheets")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListTimesheets_WithFilters_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setCrewId("crew-1");
        timesheet.setWorkDate(LocalDate.now().minusDays(1));
        timesheetRepository.save(timesheet);

        mockMvc.perform(get("/api/v1/timesheets")
                .param("orgId", testOrganization.getId().toString())
                .param("crewId", " crew-1 ")
                .param("status", "draft")
                .param("dateFrom", LocalDate.now().minusDays(2).toString())
                .param("dateTo", LocalDate.now().toString())
                .param("includeArchived", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateTimesheet_WithValidData_ReturnsCreated() throws Exception {
        CreateTimesheetDto dto = new CreateTimesheetDto(
            testOrganization.getId().toString(),
            testProject.getId().toString(),
            testPerson.getId().toString(),
            "crew-1",
            LocalDate.now(),
            List.of(new TimesheetEntryDto("task-1", 8.0, "regular", null)),
            testPerson.getId().toString()
        );

        mockMvc.perform(post("/api/v1/timesheets")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetTimesheetById_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        mockMvc.perform(get("/api/v1/timesheets/" + timesheet.getId())
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(timesheet.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateTimesheet_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        UpdateTimesheetDto dto = new UpdateTimesheetDto(null, "approved");

        mockMvc.perform(patch("/api/v1/timesheets/" + timesheet.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("approved"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testSubmitApproveTimesheetFlow_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/submit")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/approve")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"approverId\":\"1\"}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("approved"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testRejectTimesheet_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/submit")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/reject")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"approverId\":\"1\",\"reason\":\"Missing details\"}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rejected"))
            .andExpect(jsonPath("$.rejectionReason").value("Missing details"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveUnarchiveTimesheet_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/timesheets/" + timesheet.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEvents_ReturnsOk() throws Exception {
        Timesheet timesheet = new Timesheet();
        timesheet.setOrganization(testOrganization);
        timesheet.setProject(testProject);
        timesheet.setPerson(testPerson);
        timesheet.setWorkDate(LocalDate.now());
        timesheet = timesheetRepository.save(timesheet);

        mockMvc.perform(get("/api/v1/timesheets/" + timesheet.getId() + "/events")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}

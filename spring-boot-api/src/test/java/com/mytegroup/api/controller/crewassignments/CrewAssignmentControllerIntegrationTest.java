package com.mytegroup.api.controller.crewassignments;

import com.mytegroup.api.dto.crewassignments.CreateCrewAssignmentDto;
import com.mytegroup.api.dto.crewassignments.UpdateCrewAssignmentDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewAssignment;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.repository.crew.CrewAssignmentRepository;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CrewAssignmentControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Office testOffice;
    private Project testProject;
    private Person testPerson;

    @Autowired
    private CrewAssignmentRepository crewAssignmentRepository;

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
    void testListAssignments_WithOrgAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/crew-assignments")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListAssignments_WithFilters_ReturnsOk() throws Exception {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(testOrganization);
        assignment.setProject(testProject);
        assignment.setPerson(testPerson);
        assignment.setCrewId("crew-1");
        assignment.setStartDate(LocalDate.now().minusDays(2));
        assignment.setEndDate(LocalDate.now().minusDays(1));
        crewAssignmentRepository.save(assignment);

        mockMvc.perform(get("/api/v1/crew-assignments")
                .param("orgId", testOrganization.getId().toString())
                .param("crewId", " crew-1 ")
                .param("status", "active")
                .param("dateFrom", LocalDate.now().minusDays(3).toString())
                .param("dateTo", LocalDate.now().toString())
                .param("includeArchived", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateAssignment_WithValidData_ReturnsCreated() throws Exception {
        CreateCrewAssignmentDto dto = new CreateCrewAssignmentDto(
            testOrganization.getId().toString(),
            testProject.getId().toString(),
            testPerson.getId().toString(),
            "crew-1",
            "foreman",
            LocalDate.now(),
            null,
            testPerson.getId().toString()
        );

        mockMvc.perform(post("/api/v1/crew-assignments")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetAssignmentById_ReturnsOk() throws Exception {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(testOrganization);
        assignment.setProject(testProject);
        assignment.setPerson(testPerson);
        assignment.setCrewId("crew-1");
        assignment.setStartDate(LocalDate.now());
        assignment = crewAssignmentRepository.save(assignment);

        mockMvc.perform(get("/api/v1/crew-assignments/" + assignment.getId())
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignment.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testUpdateAssignment_ReturnsOk() throws Exception {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(testOrganization);
        assignment.setProject(testProject);
        assignment.setPerson(testPerson);
        assignment.setCrewId("crew-1");
        assignment.setStartDate(LocalDate.now());
        assignment = crewAssignmentRepository.save(assignment);

        UpdateCrewAssignmentDto dto = new UpdateCrewAssignmentDto("crew-2", "lead", null, null, "active");

        mockMvc.perform(patch("/api/v1/crew-assignments/" + assignment.getId())
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.crewId").value("crew-2"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveUnarchiveAssignment_ReturnsOk() throws Exception {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(testOrganization);
        assignment.setProject(testProject);
        assignment.setPerson(testPerson);
        assignment.setCrewId("crew-1");
        assignment.setStartDate(LocalDate.now());
        assignment = crewAssignmentRepository.save(assignment);

        mockMvc.perform(post("/api/v1/crew-assignments/" + assignment.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/crew-assignments/" + assignment.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEvents_ReturnsOk() throws Exception {
        CrewAssignment assignment = new CrewAssignment();
        assignment.setOrganization(testOrganization);
        assignment.setProject(testProject);
        assignment.setPerson(testPerson);
        assignment.setCrewId("crew-1");
        assignment.setStartDate(LocalDate.now());
        assignment = crewAssignmentRepository.save(assignment);

        mockMvc.perform(get("/api/v1/crew-assignments/" + assignment.getId() + "/events")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}

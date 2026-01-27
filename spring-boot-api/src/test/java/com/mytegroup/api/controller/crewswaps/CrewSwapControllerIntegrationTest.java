package com.mytegroup.api.controller.crewswaps;

import com.mytegroup.api.dto.crewswaps.CreateCrewSwapDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.crew.CrewSwap;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.repository.crew.CrewSwapRepository;
import com.mytegroup.api.test.controller.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CrewSwapControllerIntegrationTest extends BaseControllerTest {

    private Organization testOrganization;
    private Office testOffice;
    private Project testProject;
    private Person testPerson;

    @Autowired
    private CrewSwapRepository crewSwapRepository;

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
    void testListSwaps_WithOrgAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/crew-swaps")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testListSwaps_WithFilters_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        crewSwapRepository.save(swap);

        mockMvc.perform(get("/api/v1/crew-swaps")
                .param("orgId", testOrganization.getId().toString())
                .param("fromCrewId", " crew-a ")
                .param("toCrewId", " crew-b ")
                .param("status", "requested")
                .param("includeArchived", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCreateSwap_WithValidData_ReturnsCreated() throws Exception {
        CreateCrewSwapDto dto = new CreateCrewSwapDto(
            testOrganization.getId().toString(),
            testProject.getId().toString(),
            testPerson.getId().toString(),
            "crew-a",
            "crew-b",
            testPerson.getId().toString(),
            LocalDateTime.now()
        );

        mockMvc.perform(post("/api/v1/crew-swaps")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testGetSwapById_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(get("/api/v1/crew-swaps/" + swap.getId())
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(swap.getId()));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testApproveCompleteSwap_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/approve")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"approverId\":\"1\"}")
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/complete")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"completedBy\":\"1\"}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testRejectSwap_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/reject")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"approverId\":\"1\",\"reason\":\"Not needed\"}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rejected"))
            .andExpect(jsonPath("$.rejectionReason").value("Not needed"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testCancelSwap_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/cancel")
                .param("orgId", testOrganization.getId().toString())
                .contentType(APPLICATION_JSON)
                .content("{\"canceledBy\":\"1\",\"reason\":\"Schedule change\"}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("canceled"));
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testArchiveUnarchiveSwap_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/archive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/crew-swaps/" + swap.getId() + "/unarchive")
                .param("orgId", testOrganization.getId().toString())
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = ROLE_ORG_ADMIN)
    void testEvents_ReturnsOk() throws Exception {
        CrewSwap swap = new CrewSwap();
        swap.setOrganization(testOrganization);
        swap.setProject(testProject);
        swap.setPerson(testPerson);
        swap.setFromCrewId("crew-a");
        swap.setToCrewId("crew-b");
        swap.setRequestedAt(LocalDateTime.now());
        swap = crewSwapRepository.save(swap);

        mockMvc.perform(get("/api/v1/crew-swaps/" + swap.getId() + "/events")
                .param("orgId", testOrganization.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}

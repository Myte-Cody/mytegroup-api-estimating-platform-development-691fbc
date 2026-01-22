package com.mytegroup.api;

import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.repository.*;
import com.mytegroup.api.repository.companies.CompanyLocationRepository;
import com.mytegroup.api.repository.companies.CompanyRepository;
import com.mytegroup.api.repository.communication.ContactInquiryRepository;
import com.mytegroup.api.repository.communication.EmailTemplateRepository;
import com.mytegroup.api.repository.communication.NotificationRepository;
import com.mytegroup.api.repository.core.InviteRepository;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.core.WaitlistEntryRepository;
import com.mytegroup.api.repository.cost.CostCodeImportJobRepository;
import com.mytegroup.api.repository.cost.CostCodeRepository;
import com.mytegroup.api.repository.legal.LegalAcceptanceRepository;
import com.mytegroup.api.repository.legal.LegalDocRepository;
import com.mytegroup.api.repository.organization.GraphEdgeRepository;
import com.mytegroup.api.repository.organization.OfficeRepository;
import com.mytegroup.api.repository.organization.OrgTaxonomyRepository;
import com.mytegroup.api.repository.people.ContactRepository;
import com.mytegroup.api.repository.people.PersonRepository;
import com.mytegroup.api.repository.projects.EstimateRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.repository.projects.SeatRepository;
import com.mytegroup.api.repository.system.EventLogRepository;
import com.mytegroup.api.repository.system.TenantMigrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for service integration tests.
 * Provides Testcontainers setup, database cleanup, and test data helper methods.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    // Repositories for cleanup - ordered by dependency (child to parent)
    @Autowired
    protected CompanyLocationRepository companyLocationRepository;
    @Autowired
    protected ContactRepository contactRepository;
    @Autowired
    protected PersonRepository personRepository;
    @Autowired
    protected EstimateRepository estimateRepository;
    @Autowired
    protected SeatRepository seatRepository;
    @Autowired
    protected ProjectRepository projectRepository;
    @Autowired
    protected GraphEdgeRepository graphEdgeRepository;
    @Autowired
    protected OfficeRepository officeRepository;
    @Autowired
    protected OrgTaxonomyRepository orgTaxonomyRepository;
    @Autowired
    protected CompanyRepository companyRepository;
    @Autowired
    protected CostCodeRepository costCodeRepository;
    @Autowired
    protected CostCodeImportJobRepository costCodeImportJobRepository;
    @Autowired
    protected ContactInquiryRepository contactInquiryRepository;
    @Autowired
    protected EmailTemplateRepository emailTemplateRepository;
    @Autowired
    protected NotificationRepository notificationRepository;
    @Autowired
    protected LegalAcceptanceRepository legalAcceptanceRepository;
    @Autowired
    protected LegalDocRepository legalDocRepository;
    @Autowired
    protected InviteRepository inviteRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected WaitlistEntryRepository waitlistEntryRepository;
    @Autowired
    protected OrganizationRepository organizationRepository;
    @Autowired
    protected AuditRepository auditRepository;
    @Autowired
    protected EventLogRepository eventLogRepository;
    @Autowired
    protected TenantMigrationRepository tenantMigrationRepository;

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        cleanupDatabase();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        cleanupDatabase();
    }

    /**
     * Cleans up the database by deleting all entities in reverse dependency order.
     * This ensures foreign key constraints are respected.
     */
    protected void cleanupDatabase() {
        // Delete child entities first
        companyLocationRepository.deleteAll();
        contactRepository.deleteAll();
        personRepository.deleteAll();
        estimateRepository.deleteAll();
        seatRepository.deleteAll();
        projectRepository.deleteAll();
        graphEdgeRepository.deleteAll();
        officeRepository.deleteAll();
        orgTaxonomyRepository.deleteAll();
        companyRepository.deleteAll();
        costCodeRepository.deleteAll();
        costCodeImportJobRepository.deleteAll();
        contactInquiryRepository.deleteAll();
        emailTemplateRepository.deleteAll();
        notificationRepository.deleteAll();
        legalAcceptanceRepository.deleteAll();
        legalDocRepository.deleteAll();
        inviteRepository.deleteAll();
        
        // Delete core entities
        userRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        organizationRepository.deleteAll();
        
        // Delete audit and system tables
        auditRepository.deleteAll();
        eventLogRepository.deleteAll();
        tenantMigrationRepository.deleteAll();
    }

    /**
     * Creates a test organization.
     * @param name Organization name
     * @return Created organization
     */
    protected Organization setupOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setPrimaryDomain(name.toLowerCase().replaceAll("\\s+", "") + ".test");
        return organizationRepository.save(org);
    }

    /**
     * Creates a test organization with default name.
     * @return Created organization
     */
    protected Organization setupOrganization() {
        return setupOrganization("Test Organization");
    }

    /**
     * Creates a test user.
     * @param organization Organization to associate user with
     * @param email User email
     * @return Created user
     */
    protected User setupUser(Organization organization, String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email); // Use email as username for tests
        user.setOrganization(organization);
        user.setPasswordHash("$2a$10$dummyHashForTesting");
        user.setIsEmailVerified(true);
        return userRepository.save(user);
    }

    /**
     * Creates a test user with default email.
     * @param organization Organization to associate user with
     * @return Created user
     */
    protected User setupUser(Organization organization) {
        return setupUser(organization, "test@example.com");
    }

    /**
     * Creates a test company.
     * @param organization Organization to associate company with
     * @param name Company name
     * @return Created company
     */
    protected Company setupCompany(Organization organization, String name) {
        Company company = new Company();
        company.setName(name);
        company.setNormalizedName(name.toLowerCase().replaceAll("\\s+", ""));
        company.setOrganization(organization);
        return companyRepository.save(company);
    }

    /**
     * Creates a test company with default name.
     * @param organization Organization to associate company with
     * @return Created company
     */
    protected Company setupCompany(Organization organization) {
        return setupCompany(organization, "Test Company");
    }

    /**
     * Creates a test company location.
     * @param company Company to associate location with
     * @param name Location name
     * @return Created company location
     */
    protected CompanyLocation setupCompanyLocation(Company company, String name) {
        CompanyLocation location = new CompanyLocation();
        location.setName(name);
        location.setNormalizedName(name.toLowerCase().replaceAll("\\s+", ""));
        location.setCompany(company);
        location.setOrganization(company.getOrganization());
        return companyLocationRepository.save(location);
    }

    /**
     * Creates a test company location with default name.
     * @param company Company to associate location with
     * @return Created company location
     */
    protected CompanyLocation setupCompanyLocation(Company company) {
        return setupCompanyLocation(company, "Test Location");
    }
}


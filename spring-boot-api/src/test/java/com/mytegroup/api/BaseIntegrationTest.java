package com.mytegroup.api;

import com.mytegroup.api.config.TestRedisConfig;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.core.InviteStatus;
import com.mytegroup.api.entity.cost.CostCode;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.people.Contact;
import com.mytegroup.api.entity.people.Person;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for service integration tests.
 * Provides Testcontainers setup, database cleanup, and test data helper methods.
 * Uses TestRedisConfig for Redis Testcontainer integration.
 * Excludes problematic Redis auto-configurations to prevent bean initialization issues.
 */
@SpringBootTest(classes = {
    com.mytegroup.api.Application.class
}, properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
    "spring.data.redis.repositories.enabled=false"
})
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    // Ensure Testcontainers are initialized before any tests run
    static {
        TestcontainersSetup.isRunning();       // Forces initialization of PostgreSQL Testcontainer
        RedisTestcontainersSetup.isRunning();  // Forces initialization of Redis Testcontainer
    }

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
        // @Transactional annotation on class handles database cleanup via rollback
        // No explicit cleanup needed here
    }

    @AfterEach
    void tearDown() {
        // @Transactional annotation on class handles rollback cleanup
        // No explicit cleanup needed here
    }

    /**
     * Cleans up the database by deleting all entities in reverse dependency order.
     * This ensures foreign key constraints are respected.
     * Note: With @Transactional, automatic rollback handles cleanup.
     * This method is kept for manual cleanup if needed.
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
     * Performs the actual database cleanup operations.
     * Silently fails on individual deletes to avoid blocking cleanup.
     */
    private void performCleanup() {
        // No longer used - relying on @Transactional rollback
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

    /**
     * Creates a test cost code with all required fields.
     * @param organization Organization to associate cost code with
     * @param code Cost code string
     * @return Created cost code
     */
    protected CostCode setupCostCode(Organization organization, String code) {
        CostCode costCode = new CostCode();
        costCode.setOrganization(organization);
        costCode.setCode(code);
        costCode.setCategory("Labor");
        costCode.setDescription("Test Cost Code: " + code); // Required field!
        costCode.setActive(true);
        return costCodeRepository.save(costCode);
    }

    /**
     * Creates a test cost code with default values.
     * @param organization Organization to associate cost code with
     * @return Created cost code
     */
    protected CostCode setupCostCode(Organization organization) {
        return setupCostCode(organization, "TEST-001");
    }

    /**
     * Creates a test office with all required fields.
     * @param organization Organization to associate office with
     * @param name Office name
     * @return Created office
     */
    protected Office setupOffice(Organization organization, String name) {
        Office office = new Office();
        office.setOrganization(organization);
        office.setName(name);
        office.setNormalizedName(name.toLowerCase().replaceAll("\\s+", ""));
        office.setAddress("123 Test Street");
        office.setDescription("Test office");
        return officeRepository.save(office);
    }

    /**
     * Creates a test office with default values.
     * @param organization Organization to associate office with
     * @return Created office
     */
    protected Office setupOffice(Organization organization) {
        return setupOffice(organization, "Test Office");
    }

    /**
     * Creates a test person with all required fields.
     * @param organization Organization to associate person with
     * @param firstName Person's first name
     * @param lastName Person's last name
     * @return Created person
     */
    protected Person setupPerson(Organization organization, String firstName, String lastName) {
        Person person = new Person();
        person.setOrganization(organization);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setDisplayName(firstName + " " + lastName);
        person.setPrimaryEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com");
        person.setPersonType(PersonType.INTERNAL_STAFF); // Required field - cannot be null
        return personRepository.save(person);
    }

    /**
     * Creates a test person with default values.
     * @param organization Organization to associate person with
     * @return Created person
     */
    protected Person setupPerson(Organization organization) {
        return setupPerson(organization, "John", "Doe");
    }

    /**
     * Creates a test project with all required fields.
     * @param organization Organization to associate project with
     * @param office Office to associate project with
     * @param name Project name
     * @return Created project
     */
    protected Project setupProject(Organization organization, Office office, String name) {
        Project project = new Project();
        project.setOrganization(organization);
        project.setOffice(office);
        project.setName(name);
        return projectRepository.save(project);
    }

    /**
     * Creates a test project with default values.
     * @param organization Organization to associate project with
     * @param office Office to associate project with
     * @return Created project
     */
    protected Project setupProject(Organization organization, Office office) {
        return setupProject(organization, office, "Test Project");
    }

    /**
     * Creates a test estimate with all required fields.
     * @param organization Organization to associate estimate with
     * @param project Project to associate estimate with
     * @param name Estimate name
     * @return Created estimate
     */
    protected Estimate setupEstimate(Organization organization, Project project, String name) {
        // Create a default user for createdByUser if needed
        User createdByUser = setupUser(organization, "estimator@test.com");
        
        Estimate estimate = new Estimate();
        estimate.setOrganization(organization);
        estimate.setProject(project);
        estimate.setName(name);
        estimate.setCreatedByUser(createdByUser); // Required field
        return estimateRepository.save(estimate);
    }

    /**
     * Creates a test estimate with default values.
     * @param organization Organization to associate estimate with
     * @param project Project to associate estimate with
     * @return Created estimate
     */
    protected Estimate setupEstimate(Organization organization, Project project) {
        return setupEstimate(organization, project, "Test Estimate");
    }

    /**
     * Creates a test contact with all required fields.
     * @param organization Organization to associate contact with
     * @param name Contact name
     * @return Created contact
     */
    protected Contact setupContact(Organization organization, String name) {
        Contact contact = new Contact();
        contact.setOrganization(organization);
        contact.setName(name);
        contact.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        return contactRepository.save(contact);
    }

    /**
     * Creates a test contact with default values.
     * @param organization Organization to associate contact with
     * @param firstName Contact's first name
     * @param lastName Contact's last name
     * @return Created contact
     */
    protected Contact setupContact(Organization organization, String firstName, String lastName) {
        return setupContact(organization, firstName + " " + lastName);
    }

    /**
     * Creates a test invite with all required fields.
     * @param organization Organization to associate invite with
     * @param person Person to invite (optional)
     * @param email Email address for the invite
     * @param createdByUser User creating the invite
     * @return Created invite
     */
    protected Invite setupInvite(Organization organization, Person person, String email, User createdByUser) {
        Invite invite = new Invite();
        invite.setOrganization(organization);
        invite.setEmail(email);
        invite.setRole(Role.USER);
        invite.setPerson(person);
        invite.setTokenHash("test-token-hash-" + System.currentTimeMillis());
        invite.setTokenExpires(java.time.LocalDateTime.now().plusHours(72));
        invite.setStatus(InviteStatus.PENDING);
        invite.setCreatedByUser(createdByUser);
        return inviteRepository.save(invite);
    }

    /**
     * Creates a test invite with default values.
     * @param organization Organization to associate invite with
     * @param person Person to invite
     * @return Created invite
     */
    protected Invite setupInvite(Organization organization, Person person) {
        User createdByUser = setupUser(organization, "inviter@test.com");
        return setupInvite(organization, person, person.getPrimaryEmail(), createdByUser);
    }

    /**
     * Creates a test email template with all required fields.
     * @param organization Organization to associate template with
     * @param name Template name
     * @param locale Template locale (defaults to "en")
     * @return Created email template
     */
    protected com.mytegroup.api.entity.communication.EmailTemplate setupEmailTemplate(
            Organization organization, String name, String locale) {
        // Ensure organization is persisted first
        if (organization.getId() == null) {
            organization = organizationRepository.save(organization);
        }
        
        com.mytegroup.api.entity.communication.EmailTemplate template = 
            new com.mytegroup.api.entity.communication.EmailTemplate();
        template.setOrganization(organization);
        template.setName(name);
        template.setLocale(locale != null ? locale : "en");
        template.setSubject("Test Subject for " + name);
        template.setHtml("<html><body><h1>Test HTML for " + name + "</h1></body></html>");
        template.setText("Test text for " + name);
        template.setRequiredVariables(new java.util.ArrayList<>());
        template.setOptionalVariables(new java.util.ArrayList<>());
        template = emailTemplateRepository.save(template);
        // Flush to ensure it's persisted before queries
        emailTemplateRepository.flush();
        return template;
    }

    /**
     * Creates a test email template with default locale.
     * @param organization Organization to associate template with
     * @param name Template name
     * @return Created email template
     */
    protected com.mytegroup.api.entity.communication.EmailTemplate setupEmailTemplate(
            Organization organization, String name) {
        return setupEmailTemplate(organization, name, "en");
    }
}


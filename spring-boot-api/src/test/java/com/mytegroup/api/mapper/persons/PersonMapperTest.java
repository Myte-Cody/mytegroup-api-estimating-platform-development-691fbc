package com.mytegroup.api.mapper.persons;

import com.mytegroup.api.dto.persons.CreatePersonDto;
import com.mytegroup.api.dto.persons.PersonCertificationDto;
import com.mytegroup.api.dto.persons.UpdatePersonDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.enums.people.PersonType;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.people.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PersonMapper.
 */
class PersonMapperTest {

    private PersonMapper mapper;
    private Organization testOrg;
    private Office testOffice;
    private Company testCompany;
    private CompanyLocation testLocation;
    private Person reportsTo;

    @BeforeEach
    void setUp() {
        mapper = new PersonMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testOffice = new Office();
        testOffice.setId(1L);
        testCompany = new Company();
        testCompany.setId(1L);
        testLocation = new CompanyLocation();
        testLocation.setId(1L);
        reportsTo = new Person();
        reportsTo.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreatePersonDto dto = new CreatePersonDto(
                PersonType.EMPLOYEE,
                "John Doe",
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john@example.com",
                "+1234567890",
                List.of("john@example.com"),
                List.of("+1234567890"),
                List.of("tag1"),
                List.of("skill1"),
                "dept1",
                1L,
                1L,
                1L,
                1L,
                "IW-001",
                "Local 1",
                List.of("skill text"),
                4.5,
                "Notes",
                "Title",
                List.of()
        );

        // When
        Person person = mapper.toEntity(dto, testOrg, testOffice, testCompany, testLocation, reportsTo);

        // Then
        assertThat(person).isNotNull();
        assertThat(person.getPersonType()).isEqualTo(PersonType.EMPLOYEE);
        assertThat(person.getDisplayName()).isEqualTo("John Doe");
        assertThat(person.getOrganization()).isEqualTo(testOrg);
        assertThat(person.getCompany()).isEqualTo(testCompany);
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        Person person = new Person();
        person.setDisplayName("Original");
        person.setFirstName("Original");

        UpdatePersonDto dto = new UpdatePersonDto(
                null,
                "Updated",
                "Updated",
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(person, dto, null, null, null, null);

        // Then
        assertThat(person.getDisplayName()).isEqualTo("Updated");
        assertThat(person.getFirstName()).isEqualTo("Updated");
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        Person person = new Person();
        person.setDisplayName("Original");
        person.setFirstName("Original");

        UpdatePersonDto dto = new UpdatePersonDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        mapper.updateEntity(person, dto, null, null, null, null);

        // Then
        assertThat(person.getDisplayName()).isEqualTo("Original");
        assertThat(person.getFirstName()).isEqualTo("Original");
    }
}


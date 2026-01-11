package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.CreateVersionRequest;
import com.liftsimulator.admin.dto.UpdateVersionConfigRequest;
import com.liftsimulator.admin.dto.VersionResponse;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LiftSystemVersionService.
 */
@ExtendWith(MockitoExtension.class)
public class LiftSystemVersionServiceTest {

    @Mock
    private LiftSystemRepository liftSystemRepository;

    @Mock
    private LiftSystemVersionRepository versionRepository;

    @Mock
    private ConfigValidationService configValidationService;

    @InjectMocks
    private LiftSystemVersionService versionService;

    private LiftSystem mockLiftSystem;
    private LiftSystemVersion mockVersion;
    private ConfigValidationResponse validValidationResponse;

    @BeforeEach
    public void setUp() {
        mockLiftSystem = new LiftSystem();
        mockLiftSystem.setId(1L);
        mockLiftSystem.setSystemKey("test-system");
        mockLiftSystem.setDisplayName("Test System");
        mockLiftSystem.setDescription("Test Description");
        mockLiftSystem.setCreatedAt(OffsetDateTime.now());
        mockLiftSystem.setUpdatedAt(OffsetDateTime.now());

        mockVersion = new LiftSystemVersion();
        mockVersion.setId(1L);
        mockVersion.setLiftSystem(mockLiftSystem);
        mockVersion.setVersionNumber(1);
        mockVersion.setConfig("{\"floors\": 10}");
        mockVersion.setStatus(VersionStatus.DRAFT);
        mockVersion.setIsPublished(false);
        mockVersion.setCreatedAt(OffsetDateTime.now());
        mockVersion.setUpdatedAt(OffsetDateTime.now());

        validValidationResponse = new ConfigValidationResponse(
            true,
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    @Test
    public void testCreateVersion_WithConfig() {
        String config = "{\"floors\": 10, \"lifts\": 2}";
        CreateVersionRequest request = new CreateVersionRequest(config, null);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(configValidationService.validate(anyString())).thenReturn(validValidationResponse);
        when(versionRepository.findMaxVersionNumberByLiftSystemId(1L)).thenReturn(null);
        when(versionRepository.save(any(LiftSystemVersion.class))).thenReturn(mockVersion);

        VersionResponse response = versionService.createVersion(1L, request);

        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        verify(liftSystemRepository).findById(1L);
        verify(configValidationService).validate(config);
        verify(versionRepository).findMaxVersionNumberByLiftSystemId(1L);
        verify(versionRepository).save(any(LiftSystemVersion.class));
    }

    @Test
    public void testCreateVersion_WithCloning() {
        String originalConfig = "{\"floors\": 10}";
        LiftSystemVersion sourceVersion = new LiftSystemVersion();
        sourceVersion.setConfig(originalConfig);

        CreateVersionRequest request = new CreateVersionRequest("{}", 1);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(sourceVersion));
        when(configValidationService.validate(originalConfig)).thenReturn(validValidationResponse);
        when(versionRepository.findMaxVersionNumberByLiftSystemId(1L)).thenReturn(1);
        when(versionRepository.save(any(LiftSystemVersion.class))).thenReturn(mockVersion);

        VersionResponse response = versionService.createVersion(1L, request);

        assertNotNull(response);
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(configValidationService).validate(originalConfig);
        verify(versionRepository).save(any(LiftSystemVersion.class));
    }

    @Test
    public void testCreateVersion_LiftSystemNotFound() {
        CreateVersionRequest request = new CreateVersionRequest("{\"floors\": 10}", null);

        when(liftSystemRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.createVersion(999L, request)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).findById(999L);
    }

    @Test
    public void testCreateVersion_CloneSourceNotFound() {
        CreateVersionRequest request = new CreateVersionRequest("{}", 999);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 999))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.createVersion(1L, request)
        );

        assertEquals("Version 999 not found for lift system 1", exception.getMessage());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 999);
    }

    @Test
    public void testCreateVersion_VersionNumberIncrement() {
        CreateVersionRequest request = new CreateVersionRequest("{\"floors\": 15}", null);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(configValidationService.validate(anyString())).thenReturn(validValidationResponse);
        when(versionRepository.findMaxVersionNumberByLiftSystemId(1L)).thenReturn(5);

        LiftSystemVersion newVersion = new LiftSystemVersion();
        newVersion.setId(6L);
        newVersion.setLiftSystem(mockLiftSystem);
        newVersion.setVersionNumber(6);
        newVersion.setConfig("{\"floors\": 15}");
        newVersion.setStatus(VersionStatus.DRAFT);
        newVersion.setIsPublished(false);
        newVersion.setCreatedAt(OffsetDateTime.now());
        newVersion.setUpdatedAt(OffsetDateTime.now());

        when(versionRepository.save(any(LiftSystemVersion.class))).thenReturn(newVersion);

        VersionResponse response = versionService.createVersion(1L, request);

        assertNotNull(response);
        assertEquals(6, response.versionNumber());
        verify(versionRepository).findMaxVersionNumberByLiftSystemId(1L);
    }

    @Test
    public void testUpdateVersionConfig_Success() {
        String updatedConfig = "{\"floors\": 20, \"lifts\": 3}";
        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest(updatedConfig);

        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));
        when(configValidationService.validate(updatedConfig)).thenReturn(validValidationResponse);
        when(versionRepository.save(any(LiftSystemVersion.class))).thenReturn(mockVersion);

        VersionResponse response = versionService.updateVersionConfig(1L, 1, request);

        assertNotNull(response);
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(configValidationService).validate(updatedConfig);
        verify(versionRepository).save(mockVersion);
    }

    @Test
    public void testUpdateVersionConfig_VersionNotFound() {
        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest("{\"floors\": 20}");

        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 999))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.updateVersionConfig(1L, 999, request)
        );

        assertEquals("Version 999 not found for lift system 1", exception.getMessage());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 999);
    }

    @Test
    public void testListVersions_Success() {
        List<LiftSystemVersion> versions = List.of(mockVersion);

        when(liftSystemRepository.existsById(1L)).thenReturn(true);
        when(versionRepository.findByLiftSystemIdOrderByVersionNumberDesc(1L))
            .thenReturn(versions);

        List<VersionResponse> responses = versionService.listVersions(1L);

        assertEquals(1, responses.size());
        assertEquals(1, responses.get(0).versionNumber());
        verify(liftSystemRepository).existsById(1L);
        verify(versionRepository).findByLiftSystemIdOrderByVersionNumberDesc(1L);
    }

    @Test
    public void testListVersions_LiftSystemNotFound() {
        when(liftSystemRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.listVersions(999L)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).existsById(999L);
    }

    @Test
    public void testGetVersion_Success() {
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));

        VersionResponse response = versionService.getVersion(1L, 1);

        assertNotNull(response);
        assertEquals(1, response.versionNumber());
        assertEquals(1L, response.liftSystemId());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
    }

    @Test
    public void testGetVersion_NotFound() {
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 999))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> versionService.getVersion(1L, 999)
        );

        assertEquals("Version 999 not found for lift system 1", exception.getMessage());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 999);
    }

    @Test
    public void testPublishVersion_Success() {
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));
        when(configValidationService.validate(anyString())).thenReturn(validValidationResponse);
        when(versionRepository.save(any(LiftSystemVersion.class))).thenReturn(mockVersion);

        VersionResponse response = versionService.publishVersion(1L, 1);

        assertNotNull(response);
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(configValidationService).validate(mockVersion.getConfig());
        verify(versionRepository).save(mockVersion);
    }

    @Test
    public void testPublishVersion_AlreadyPublished() {
        mockVersion.setIsPublished(true);

        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> versionService.publishVersion(1L, 1)
        );

        assertEquals("Version 1 is already published", exception.getMessage());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
    }

    @Test
    public void testPublishVersion_ValidationFails() {
        ConfigValidationResponse invalidResponse = new ConfigValidationResponse(
            false,
            List.of(new com.liftsimulator.admin.dto.ValidationIssue(
                "floors",
                "Number of floors must be at least 2",
                com.liftsimulator.admin.dto.ValidationIssue.Severity.ERROR
            )),
            Collections.emptyList()
        );

        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));
        when(configValidationService.validate(anyString())).thenReturn(invalidResponse);

        ConfigValidationException exception = assertThrows(
            ConfigValidationException.class,
            () -> versionService.publishVersion(1L, 1)
        );

        assertEquals("Cannot publish version with validation errors", exception.getMessage());
        assertTrue(exception.getValidationResponse().hasErrors());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(configValidationService).validate(mockVersion.getConfig());
    }

    @Test
    public void testCreateVersion_ValidationFails() {
        String invalidConfig = "{\"floors\": 1}";
        CreateVersionRequest request = new CreateVersionRequest(invalidConfig, null);

        ConfigValidationResponse invalidResponse = new ConfigValidationResponse(
            false,
            List.of(new com.liftsimulator.admin.dto.ValidationIssue(
                "floors",
                "Number of floors must be at least 2",
                com.liftsimulator.admin.dto.ValidationIssue.Severity.ERROR
            )),
            Collections.emptyList()
        );

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(configValidationService.validate(invalidConfig)).thenReturn(invalidResponse);

        ConfigValidationException exception = assertThrows(
            ConfigValidationException.class,
            () -> versionService.createVersion(1L, request)
        );

        assertEquals("Configuration validation failed", exception.getMessage());
        assertTrue(exception.getValidationResponse().hasErrors());
        verify(liftSystemRepository).findById(1L);
        verify(configValidationService).validate(invalidConfig);
    }

    @Test
    public void testUpdateVersionConfig_ValidationFails() {
        String invalidConfig = "{\"floors\": 1}";
        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest(invalidConfig);

        ConfigValidationResponse invalidResponse = new ConfigValidationResponse(
            false,
            List.of(new com.liftsimulator.admin.dto.ValidationIssue(
                "floors",
                "Number of floors must be at least 2",
                com.liftsimulator.admin.dto.ValidationIssue.Severity.ERROR
            )),
            Collections.emptyList()
        );

        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockVersion));
        when(configValidationService.validate(invalidConfig)).thenReturn(invalidResponse);

        ConfigValidationException exception = assertThrows(
            ConfigValidationException.class,
            () -> versionService.updateVersionConfig(1L, 1, request)
        );

        assertEquals("Configuration validation failed", exception.getMessage());
        assertTrue(exception.getValidationResponse().hasErrors());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(configValidationService).validate(invalidConfig);
    }
}

package com.liftsimulator.runtime.service;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.service.ResourceNotFoundException;
import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RuntimeConfigService.
 */
@ExtendWith(MockitoExtension.class)
public class RuntimeConfigServiceTest {

    @Mock
    private LiftSystemRepository liftSystemRepository;

    @Mock
    private LiftSystemVersionRepository versionRepository;

    @InjectMocks
    private RuntimeConfigService runtimeConfigService;

    private LiftSystem mockLiftSystem;
    private LiftSystemVersion mockPublishedVersion;

    @BeforeEach
    public void setUp() {
        mockLiftSystem = new LiftSystem();
        mockLiftSystem.setId(1L);
        mockLiftSystem.setSystemKey("test-system");
        mockLiftSystem.setDisplayName("Test System");
        mockLiftSystem.setDescription("Test Description");
        mockLiftSystem.setCreatedAt(OffsetDateTime.now());
        mockLiftSystem.setUpdatedAt(OffsetDateTime.now());

        mockPublishedVersion = new LiftSystemVersion();
        mockPublishedVersion.setId(1L);
        mockPublishedVersion.setLiftSystem(mockLiftSystem);
        mockPublishedVersion.setVersionNumber(1);
        mockPublishedVersion.setConfig("{\"floors\": 10, \"lifts\": 2}");
        mockPublishedVersion.setStatus(VersionStatus.PUBLISHED);
        mockPublishedVersion.setIsPublished(true);
        mockPublishedVersion.setPublishedAt(OffsetDateTime.now());
        mockPublishedVersion.setCreatedAt(OffsetDateTime.now());
        mockPublishedVersion.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    public void testGetPublishedConfig_Success() {
        when(liftSystemRepository.findBySystemKey("test-system"))
            .thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndIsPublishedTrue(1L))
            .thenReturn(List.of(mockPublishedVersion));

        RuntimeConfigDTO result = runtimeConfigService.getPublishedConfig("test-system");

        assertNotNull(result);
        assertEquals("test-system", result.systemKey());
        assertEquals("Test System", result.displayName());
        assertEquals(1, result.versionNumber());
        assertEquals("{\"floors\": 10, \"lifts\": 2}", result.config());
        assertNotNull(result.publishedAt());

        verify(liftSystemRepository).findBySystemKey("test-system");
        verify(versionRepository).findByLiftSystemIdAndIsPublishedTrue(1L);
    }

    @Test
    public void testGetPublishedConfig_LiftSystemNotFound() {
        when(liftSystemRepository.findBySystemKey("nonexistent"))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runtimeConfigService.getPublishedConfig("nonexistent")
        );

        assertEquals("Lift system not found with key: nonexistent", exception.getMessage());
        verify(liftSystemRepository).findBySystemKey("nonexistent");
    }

    @Test
    public void testGetPublishedConfig_NoPublishedVersion() {
        when(liftSystemRepository.findBySystemKey("test-system"))
            .thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndIsPublishedTrue(1L))
            .thenReturn(Collections.emptyList());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runtimeConfigService.getPublishedConfig("test-system")
        );

        assertEquals("No published version found for lift system: test-system", exception.getMessage());
        verify(liftSystemRepository).findBySystemKey("test-system");
        verify(versionRepository).findByLiftSystemIdAndIsPublishedTrue(1L);
    }

    @Test
    public void testGetPublishedVersion_Success() {
        when(liftSystemRepository.findBySystemKey("test-system"))
            .thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockPublishedVersion));

        RuntimeConfigDTO result = runtimeConfigService.getPublishedVersion("test-system", 1);

        assertNotNull(result);
        assertEquals("test-system", result.systemKey());
        assertEquals("Test System", result.displayName());
        assertEquals(1, result.versionNumber());
        assertEquals("{\"floors\": 10, \"lifts\": 2}", result.config());
        assertNotNull(result.publishedAt());

        verify(liftSystemRepository).findBySystemKey("test-system");
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
    }

    @Test
    public void testGetPublishedVersion_LiftSystemNotFound() {
        when(liftSystemRepository.findBySystemKey("nonexistent"))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runtimeConfigService.getPublishedVersion("nonexistent", 1)
        );

        assertEquals("Lift system not found with key: nonexistent", exception.getMessage());
        verify(liftSystemRepository).findBySystemKey("nonexistent");
    }

    @Test
    public void testGetPublishedVersion_VersionNotFound() {
        when(liftSystemRepository.findBySystemKey("test-system"))
            .thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 999))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runtimeConfigService.getPublishedVersion("test-system", 999)
        );

        assertEquals("Version 999 not found for lift system: test-system", exception.getMessage());
        verify(liftSystemRepository).findBySystemKey("test-system");
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 999);
    }

    @Test
    public void testGetPublishedVersion_VersionNotPublished() {
        mockPublishedVersion.setIsPublished(false);
        mockPublishedVersion.setStatus(VersionStatus.DRAFT);

        when(liftSystemRepository.findBySystemKey("test-system"))
            .thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1))
            .thenReturn(Optional.of(mockPublishedVersion));

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runtimeConfigService.getPublishedVersion("test-system", 1)
        );

        assertEquals("Version 1 is not published for lift system: test-system", exception.getMessage());
        verify(liftSystemRepository).findBySystemKey("test-system");
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
    }
}

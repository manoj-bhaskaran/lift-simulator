package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.CreateLiftSystemRequest;
import com.liftsimulator.admin.dto.LiftSystemResponse;
import com.liftsimulator.admin.dto.UpdateLiftSystemRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LiftSystemService.
 */
@ExtendWith(MockitoExtension.class)
public class LiftSystemServiceTest {

    @Mock
    private LiftSystemRepository liftSystemRepository;

    @Mock
    private LiftSystemVersionRepository liftSystemVersionRepository;

    @InjectMocks
    private LiftSystemService liftSystemService;

    private LiftSystem mockLiftSystem;

    @BeforeEach
    public void setUp() {
        mockLiftSystem = new LiftSystem();
        mockLiftSystem.setId(1L);
        mockLiftSystem.setSystemKey("test-system");
        mockLiftSystem.setDisplayName("Test System");
        mockLiftSystem.setDescription("Test Description");
        mockLiftSystem.setCreatedAt(OffsetDateTime.now());
        mockLiftSystem.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    public void testCreateLiftSystem_Success() {
        CreateLiftSystemRequest request = new CreateLiftSystemRequest(
            "new-system",
            "New System",
            "New Description"
        );

        when(liftSystemRepository.existsBySystemKey("new-system")).thenReturn(false);
        when(liftSystemRepository.save(any(LiftSystem.class))).thenReturn(mockLiftSystem);

        LiftSystemResponse response = liftSystemService.createLiftSystem(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(liftSystemRepository).existsBySystemKey("new-system");
        verify(liftSystemRepository).save(any(LiftSystem.class));
    }

    @Test
    public void testCreateLiftSystem_DuplicateKey() {
        CreateLiftSystemRequest request = new CreateLiftSystemRequest(
            "existing-system",
            "Existing System",
            "Description"
        );

        when(liftSystemRepository.existsBySystemKey("existing-system")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> liftSystemService.createLiftSystem(request)
        );

        assertEquals(
            "Lift system with key 'existing-system' already exists",
            exception.getMessage()
        );
        verify(liftSystemRepository).existsBySystemKey("existing-system");
    }

    @Test
    public void testGetAllLiftSystems() {
        List<LiftSystem> systems = List.of(mockLiftSystem);
        when(liftSystemRepository.findAll()).thenReturn(systems);
        when(liftSystemVersionRepository.countVersionsByLiftSystemId())
            .thenReturn(List.<Object[]>of(new Object[] {1L, 1L}));

        List<LiftSystemResponse> responses = liftSystemService.getAllLiftSystems();

        assertEquals(1, responses.size());
        assertEquals("test-system", responses.get(0).systemKey());
        verify(liftSystemRepository).findAll();
    }

    @Test
    public void testGetLiftSystemById_Success() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(liftSystemVersionRepository.countByLiftSystemId(1L)).thenReturn(1L);

        LiftSystemResponse response = liftSystemService.getLiftSystemById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test-system", response.systemKey());
        verify(liftSystemRepository).findById(1L);
    }

    @Test
    public void testGetLiftSystemById_NotFound() {
        when(liftSystemRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> liftSystemService.getLiftSystemById(999L)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).findById(999L);
    }

    @Test
    public void testUpdateLiftSystem_Success() {
        UpdateLiftSystemRequest request = new UpdateLiftSystemRequest(
            "Updated Name",
            "Updated Description"
        );

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(liftSystemRepository.save(any(LiftSystem.class))).thenReturn(mockLiftSystem);
        when(liftSystemVersionRepository.countByLiftSystemId(1L)).thenReturn(1L);

        LiftSystemResponse response = liftSystemService.updateLiftSystem(1L, request);

        assertNotNull(response);
        verify(liftSystemRepository).findById(1L);
        verify(liftSystemRepository).save(mockLiftSystem);
    }

    @Test
    public void testUpdateLiftSystem_NotFound() {
        UpdateLiftSystemRequest request = new UpdateLiftSystemRequest(
            "Updated Name",
            "Updated Description"
        );

        when(liftSystemRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> liftSystemService.updateLiftSystem(999L, request)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).findById(999L);
    }

    @Test
    public void testDeleteLiftSystem_Success() {
        when(liftSystemRepository.existsById(1L)).thenReturn(true);

        liftSystemService.deleteLiftSystem(1L);

        verify(liftSystemRepository).existsById(1L);
        verify(liftSystemRepository).deleteById(1L);
    }

    @Test
    public void testDeleteLiftSystem_NotFound() {
        when(liftSystemRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> liftSystemService.deleteLiftSystem(999L)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).existsById(999L);
    }
}

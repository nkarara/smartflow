package com.smartflow.service;

import com.smartflow.dto.AppSettingDtos;
import com.smartflow.entity.AppSetting;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.repository.AppSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Paramètres applicatifs : lecture, mise à jour et ressource introuvable.
 */
@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private AppSettingRepository appSettingRepository;

    @InjectMocks
    private SettingsService settingsService;

    private AppSetting setting(String key, String value) {
        return AppSetting.builder()
                .key(key).value(value).description("Description de " + key).category("Général")
                .build();
    }

    @Test
    void listAllReturnsAllSettingsSorted() {
        when(appSettingRepository.findAllByOrderByKeyAsc())
                .thenReturn(List.of(setting("support.email", "support@x.fr"), setting("company.name", "ACME")));

        List<AppSettingDtos.SettingResponse> result = settingsService.listAll();

        assertEquals(2, result.size());
        assertEquals("support.email", result.get(0).key());
        assertEquals("ACME", result.get(1).value());
    }

    @Test
    void updatePersistsNewValue() {
        AppSetting stored = setting("sla.defaultResponseHours", "4");
        when(appSettingRepository.findByKey("sla.defaultResponseHours")).thenReturn(Optional.of(stored));
        when(appSettingRepository.save(any(AppSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        AppSettingDtos.SettingResponse response =
                settingsService.update("sla.defaultResponseHours", new AppSettingDtos.UpdateSettingRequest(" 8 "));

        assertEquals("8", response.value()); // valeur normalisée (trim)
        assertEquals("8", stored.getValue());
        verify(appSettingRepository).save(stored);
    }

    @Test
    void updateOnMissingKeyThrowsNotFound() {
        when(appSettingRepository.findByKey(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> settingsService.update("inconnu", new AppSettingDtos.UpdateSettingRequest("x")));
    }
}
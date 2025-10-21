package com.capacitorjs.plugins.localnotifications;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NotificationStorageTest {

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private NotificationStorage storage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);

        storage = new NotificationStorage(mockContext);
    }

    @Test
    public void testAppendNotifications_serializesAsGsonPojo() {
        // Arrange
        LocalNotification notification = new LocalNotification();
        notification.setId(123);
        notification.setTitle("Test Title");
        notification.setBody("Test Body");
        
        LocalNotificationSchedule schedule = new LocalNotificationSchedule();
        schedule.setAt(new Date(System.currentTimeMillis() + 10000));
        notification.setSchedule(schedule);

        List<LocalNotification> notifications = new ArrayList<>();
        notifications.add(notification);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        storage.appendNotifications(notifications);

        // Assert
        verify(mockEditor).putString(keyCaptor.capture(), valueCaptor.capture());
        verify(mockEditor).apply();

        assertEquals("123", keyCaptor.getValue());
        String savedJson = valueCaptor.getValue();
        
        // Verify it's valid JSON and contains expected fields
        assertTrue("Should contain title field", savedJson.contains("\"title\":\"Test Title\""));
        assertTrue("Should contain body field", savedJson.contains("\"body\":\"Test Body\""));
        assertTrue("Should contain schedule", savedJson.contains("\"schedule\""));
        
        // Verify it's NOT the raw source format (which would have been set separately)
        assertFalse("Should not contain source field", savedJson.contains("\"source\""));
    }

    @Test
    public void testGetSavedNotification_deserializesFromGson() throws JSONException {
        // Arrange
        String gsonJson = "{\"id\":456,\"title\":\"Gson Test\",\"body\":\"From Gson\",\"schedule\":{\"at\":\"2025-12-31T23:59:59.000Z\"}}";
        when(mockSharedPreferences.getString(eq("456"), eq(null))).thenReturn(gsonJson);

        // Act
        LocalNotification result = storage.getSavedNotification("456");

        // Assert
        assertNotNull("Notification should be loaded", result);
        assertEquals("ID should match", Integer.valueOf(456), result.getId());
        assertEquals("Title should match", "Gson Test", result.getTitle());
        assertEquals("Body should match", "From Gson", result.getBody());
        assertNotNull("Schedule should exist", result.getSchedule());
    }

    @Test
    public void testGetSavedNotification_migratesFromOldFormat() throws JSONException {
        // Arrange - Old format (raw JSObject JSON)
        String oldFormatJson = "{\"id\":789,\"title\":\"Old Format\",\"body\":\"Needs Migration\"}";
        when(mockSharedPreferences.getString(eq("789"), eq(null))).thenReturn(oldFormatJson);

        // Act
        LocalNotification result = storage.getSavedNotification("789");

        // Assert
        assertNotNull("Notification should be loaded from old format", result);
        assertEquals("ID should match", Integer.valueOf(789), result.getId());
        assertEquals("Title should match", "Old Format", result.getTitle());
    }

    @Test
    public void testGetSavedNotifications_loadsMultipleNotifications() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("1", "{\"id\":1,\"title\":\"First\"}");
        mockData.put("2", "{\"id\":2,\"title\":\"Second\"}");
        mockData.put("3", "{\"id\":3,\"title\":\"Third\"}");
        
        when(mockSharedPreferences.getAll()).thenReturn((Map) mockData);

        // Act
        List<LocalNotification> results = storage.getSavedNotifications();

        // Assert
        assertEquals("Should load 3 notifications", 3, results.size());
        assertTrue("Should contain notification with title 'First'", 
            results.stream().anyMatch(n -> "First".equals(n.getTitle())));
        assertTrue("Should contain notification with title 'Second'", 
            results.stream().anyMatch(n -> "Second".equals(n.getTitle())));
        assertTrue("Should contain notification with title 'Third'", 
            results.stream().anyMatch(n -> "Third".equals(n.getTitle())));
    }

    @Test
    public void testGetSavedNotifications_handlesMixedFormats() {
        // Arrange - Mix of old and new formats
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("1", "{\"id\":1,\"title\":\"New Format\",\"body\":\"POJO\"}"); // New Gson format
        mockData.put("2", "{\"id\":2,\"title\":\"Old Format\",\"body\":\"Raw JSON\"}"); // Old format
        
        when(mockSharedPreferences.getAll()).thenReturn((Map) mockData);

        // Act
        List<LocalNotification> results = storage.getSavedNotifications();

        // Assert
        assertEquals("Should load both notifications", 2, results.size());
        
        // Both should be loaded successfully regardless of format
        assertTrue("Should load new format", 
            results.stream().anyMatch(n -> "New Format".equals(n.getTitle())));
        assertTrue("Should load old format", 
            results.stream().anyMatch(n -> "Old Format".equals(n.getTitle())));
    }

    @Test
    public void testRoundTrip_notificationWithSchedule() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        
        // Capture what's saved
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            mockData.put(key, value);
            return mockEditor;
        }).when(mockEditor).putString(anyString(), anyString());
        
        when(mockSharedPreferences.getAll()).thenReturn((Map) mockData);
        when(mockSharedPreferences.getString(eq("999"), eq(null))).thenAnswer(inv -> mockData.get("999"));

        LocalNotification original = new LocalNotification();
        original.setId(999);
        original.setTitle("Round Trip Test");
        original.setBody("This should survive serialization");
        
        LocalNotificationSchedule schedule = new LocalNotificationSchedule();
        schedule.setAt(new Date(1735689599000L)); // 2024-12-31 23:59:59
        schedule.setRepeats(false);
        original.setSchedule(schedule);

        List<LocalNotification> toSave = new ArrayList<>();
        toSave.add(original);

        // Act - Save
        storage.appendNotifications(toSave);
        
        // Act - Load
        LocalNotification loaded = storage.getSavedNotification("999");

        // Assert
        assertNotNull("Loaded notification should not be null", loaded);
        assertEquals("ID should match", original.getId(), loaded.getId());
        assertEquals("Title should match", original.getTitle(), loaded.getTitle());
        assertEquals("Body should match", original.getBody(), loaded.getBody());
        assertNotNull("Schedule should be preserved", loaded.getSchedule());
        assertNotNull("Schedule 'at' should be preserved", loaded.getSchedule().getAt());
        assertEquals("Schedule repeats should match", original.getSchedule().getRepeats(), loaded.getSchedule().getRepeats());
    }

    @Test
    public void testDeleteNotification_removesFromStorage() {
        // Act
        storage.deleteNotification("123");

        // Assert
        verify(mockEditor).remove("123");
        verify(mockEditor).apply();
    }

    @Test
    public void testGetSavedNotification_returnsNull_whenNotFound() {
        // Arrange
        when(mockSharedPreferences.getString(eq("nonexistent"), eq(null))).thenReturn(null);

        // Act
        LocalNotification result = storage.getSavedNotification("nonexistent");

        // Assert
        assertNull("Should return null for non-existent notification", result);
    }

    @Test
    public void testGetSavedNotifications_returnsEmptyList_whenNoData() {
        // Arrange
        when(mockSharedPreferences.getAll()).thenReturn(new HashMap<>());

        // Act
        List<LocalNotification> results = storage.getSavedNotifications();

        // Assert
        assertNotNull("Should return a list", results);
        assertEquals("Should return empty list", 0, results.size());
    }

    @Test
    public void testAppendNotifications_onlySavesScheduled() {
        // Arrange
        LocalNotification scheduled = new LocalNotification();
        scheduled.setId(1);
        scheduled.setTitle("Scheduled");
        LocalNotificationSchedule schedule = new LocalNotificationSchedule();
        schedule.setAt(new Date(System.currentTimeMillis() + 10000));
        scheduled.setSchedule(schedule);

        LocalNotification unscheduled = new LocalNotification();
        unscheduled.setId(2);
        unscheduled.setTitle("Not Scheduled");
        // No schedule

        List<LocalNotification> notifications = new ArrayList<>();
        notifications.add(scheduled);
        notifications.add(unscheduled);

        // Act
        storage.appendNotifications(notifications);

        // Assert
        verify(mockEditor, times(1)).putString(anyString(), anyString());
        verify(mockEditor).putString(eq("1"), anyString()); // Only scheduled one saved
        verify(mockEditor, never()).putString(eq("2"), anyString());
    }
}


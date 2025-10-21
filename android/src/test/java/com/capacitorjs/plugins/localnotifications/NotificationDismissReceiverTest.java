package com.capacitorjs.plugins.localnotifications;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NotificationDismissReceiverTest {

    @Mock
    private Context mockContext;

    @Mock
    private Intent mockIntent;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private NotificationDismissReceiver receiver;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        receiver = new NotificationDismissReceiver();
        
        // Setup SharedPreferences mock chain
        when(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
    }

    @Test
    public void testOnReceive_withValidNotificationId_removesNotification() {
        // Arrange
        int notificationId = 123;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockContext).getSharedPreferences("NOTIFICATION_STORE", Context.MODE_PRIVATE);
        verify(mockEditor).remove("123");
        verify(mockEditor).apply();
    }

    @Test
    public void testOnReceive_withInvalidNotificationId_doesNothing() {
        // Arrange
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(Integer.MIN_VALUE);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockContext, never()).getSharedPreferences(anyString(), anyInt());
        verify(mockEditor, never()).remove(anyString());
    }

    @Test
    public void testOnReceive_withNonRemovableNotification_doesNotRemove() {
        // Arrange
        int notificationId = 456;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(false);  // Not removable

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockContext, never()).getSharedPreferences(anyString(), anyInt());
        verify(mockEditor, never()).remove(anyString());
    }

    @Test
    public void testOnReceive_withZeroNotificationId_processesCorrectly() {
        // Arrange
        int notificationId = 0;  // Edge case: valid ID
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockEditor).remove("0");
        verify(mockEditor).apply();
    }

    @Test
    public void testOnReceive_withNegativeNotificationId_processesCorrectly() {
        // Arrange
        int notificationId = -1;  // Edge case: valid negative ID
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockEditor).remove("-1");
        verify(mockEditor).apply();
    }

    @Test
    public void testOnReceive_multipleNotifications_handlesEachIndependently() {
        // Arrange & Act - First notification
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(100);
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        receiver.onReceive(mockContext, mockIntent);

        // Arrange & Act - Second notification
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(200);

        receiver.onReceive(mockContext, mockIntent);

        // Assert
        verify(mockEditor).remove("100");
        verify(mockEditor).remove("200");
        verify(mockEditor, times(2)).apply();
    }

    @Test(expected = NullPointerException.class)
    public void testOnReceive_nullIntent_throwsNullPointerException() {
        // Act & Assert - Should throw NullPointerException
        receiver.onReceive(mockContext, null);
    }

    @Test
    public void testOnReceive_correctStorageKeyUsed() {
        // Arrange
        int notificationId = 999;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert - Verify correct storage name is used
        verify(mockContext).getSharedPreferences(
            eq("NOTIFICATION_STORE"), 
            eq(Context.MODE_PRIVATE)
        );
    }

    @Test
    public void testOnReceive_editorChainCallsInCorrectOrder() {
        // Arrange
        int notificationId = 777;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert - Verify call order
        org.mockito.InOrder inOrder = inOrder(mockSharedPreferences, mockEditor);
        inOrder.verify(mockSharedPreferences).edit();
        inOrder.verify(mockEditor).remove("777");
        inOrder.verify(mockEditor).apply();
    }

    @Test
    public void testOnReceive_sendsDismissBroadcast_whenNotificationRemoved() {
        // Arrange
        int notificationId = 555;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);
        
        when(mockContext.getPackageName()).thenReturn("com.example.app");

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert - Verify broadcast was sent
        verify(mockContext).sendBroadcast(argThat(
            (Intent broadcastIntent) -> LocalNotificationManager.NOTIFICATION_DISMISSED_ACTION.equals(broadcastIntent.getAction())
                && broadcastIntent.getIntExtra(LocalNotificationManager.NOTIFICATION_INTENT_KEY, -1) == notificationId
        ));
    }

    @Test
    public void testOnReceive_setsBroadcastPackage() {
        // Arrange
        int notificationId = 888;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(true);
        
        when(mockContext.getPackageName()).thenReturn("com.test.package");

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert - Verify broadcast has correct package
        verify(mockContext).sendBroadcast(argThat(
            (Intent broadcastIntent) -> "com.test.package".equals(broadcastIntent.getPackage())
        ));
    }

    @Test
    public void testOnReceive_notSendsBroadcast_whenNotRemovable() {
        // Arrange
        int notificationId = 999;
        when(mockIntent.getIntExtra(
            eq(LocalNotificationManager.NOTIFICATION_INTENT_KEY), 
            eq(Integer.MIN_VALUE)
        )).thenReturn(notificationId);
        
        when(mockIntent.getBooleanExtra(
            eq(LocalNotificationManager.NOTIFICATION_IS_REMOVABLE_KEY), 
            eq(true)
        )).thenReturn(false);  // Not removable

        // Act
        receiver.onReceive(mockContext, mockIntent);

        // Assert - Verify no broadcast sent
        verify(mockContext, never()).sendBroadcast(any(Intent.class));
    }
}


package com.fsck.k9.notification;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.message.extractors.PreviewResult.PreviewType;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NotificationContentCreatorTest extends RobolectricTest {
    private static final String ACCOUNT_UUID = "1-2-3";
    private static final long FOLDER_ID = 23;
    private static final String FOLDER_NAME = "INBOX";
    private static final String UID = "42";
    private static final String PREVIEW = "Message preview text";
    private static final String SUBJECT = "Message subject";
    private static final String SENDER_ADDRESS = "alice@example.com";
    private static final String SENDER_NAME = "Alice";
    private static final String RECIPIENT_ADDRESS = "bob@example.com";
    private static final String RECIPIENT_NAME = "Bob";


    private NotificationResourceProvider resourceProvider = new TestNotificationResourceProvider();
    private NotificationContentCreator contentCreator;
    private MessageReference messageReference;
    private Account account;
    private LocalMessage message;


    @Before
    public void setUp() throws Exception {
        contentCreator = createNotificationContentCreator();
        messageReference = createMessageReference();
        account = createFakeAccount();
        message = createFakeLocalMessage(messageReference);
    }

    @Test
    public void createFromMessage_withRegularMessage() throws Exception {
        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals(messageReference, content.getMessageReference());
        assertEquals(SENDER_NAME, content.getSender());
        assertEquals(SUBJECT, content.getSubject());
        assertEquals(SUBJECT + "\n" + PREVIEW, content.getPreview().toString());
        assertEquals(SENDER_NAME + " " + SUBJECT, content.getSummary().toString());
        assertEquals(false, content.isStarred());
    }

    @Test
    public void createFromMessage_withoutSubject() throws Exception {
        when(message.getSubject()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        String noSubject = "(No subject)";
        assertEquals(noSubject, content.getSubject());
        assertEquals(PREVIEW, content.getPreview().toString());
        assertEquals(SENDER_NAME + " " + noSubject, content.getSummary().toString());
    }

    @Test
    public void createFromMessage_withoutPreview() throws Exception {
        when(message.getPreviewType()).thenReturn(PreviewType.NONE);
        when(message.getPreview()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals(SUBJECT, content.getSubject());
        assertEquals(SUBJECT, content.getPreview().toString());
    }

    @Test
    public void createFromMessage_withErrorPreview() throws Exception {
        when(message.getPreviewType()).thenReturn(PreviewType.ERROR);
        when(message.getPreview()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals(SUBJECT, content.getSubject());
        assertEquals(SUBJECT, content.getPreview().toString());
    }

    @Test
    public void createFromMessage_withEncryptedMessage() throws Exception {
        when(message.getPreviewType()).thenReturn(PreviewType.ENCRYPTED);
        when(message.getPreview()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        String encrypted = "*Encrypted*";
        assertEquals(SUBJECT, content.getSubject());
        assertEquals(SUBJECT + "\n" + encrypted, content.getPreview().toString());
    }

    @Test
    public void createFromMessage_withoutSender() throws Exception {
        when(message.getFrom()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals("No sender", content.getSender());
        assertEquals(SUBJECT, content.getSummary().toString());
    }

    @Test
    public void createFromMessage_withMessageFromSelf() throws Exception {
        when(account.isAnIdentity(any(Address[].class))).thenReturn(true);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        String insteadOfSender = "To:Bob";
        assertEquals(insteadOfSender, content.getSender());
        assertEquals(insteadOfSender + " " + SUBJECT, content.getSummary().toString());
    }

    @Test
    public void createFromMessage_withStarredMessage() throws Exception {
        when(message.isSet(Flag.FLAGGED)).thenReturn(true);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals(true, content.isStarred());
    }

    @Test
    public void createFromMessage_withoutEmptyMessage() throws Exception {
        when(message.getFrom()).thenReturn(null);
        when(message.getSubject()).thenReturn(null);
        when(message.getPreviewType()).thenReturn(PreviewType.NONE);
        when(message.getPreview()).thenReturn(null);

        NotificationContent content = contentCreator.createFromMessage(account, message);

        assertEquals("No sender", content.getSender());
        assertEquals("(No subject)", content.getSubject());
        assertEquals("(No subject)", content.getPreview().toString());
        assertEquals("(No subject)", content.getSummary().toString());
    }

    private NotificationContentCreator createNotificationContentCreator() {
        Context context = RuntimeEnvironment.application;
        return new NotificationContentCreator(context, resourceProvider);
    }

    private Account createFakeAccount() {
        return mock(Account.class);
    }

    private MessageReference createMessageReference() {
        return new MessageReference(ACCOUNT_UUID, FOLDER_ID, UID, null);
    }

    private LocalMessage createFakeLocalMessage(MessageReference messageReference) {
        LocalMessage message = mock(LocalMessage.class);

        when(message.makeMessageReference()).thenReturn(messageReference);
        when(message.getPreviewType()).thenReturn(PreviewType.TEXT);
        when(message.getPreview()).thenReturn(PREVIEW);
        when(message.getSubject()).thenReturn(SUBJECT);
        when(message.getFrom()).thenReturn(new Address[] { new Address(SENDER_ADDRESS, SENDER_NAME) });
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new Address[] { new Address(RECIPIENT_ADDRESS, RECIPIENT_NAME) });

        return message;
    }
}

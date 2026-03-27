package com.securevote.backend.service;

import com.securevote.backend.dto.ElectionLifecycleNotification;
import com.securevote.backend.dto.RealtimeAlertEvent;
import com.securevote.backend.dto.VoteLiveUpdate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishVoteUpdate(VoteLiveUpdate update) {
        messagingTemplate.convertAndSend("/topic/admin/votes", update);
        messagingTemplate.convertAndSend("/topic/elections/" + update.getElectionId() + "/votes", update);
    }

    public void publishAlert(RealtimeAlertEvent alert) {
        messagingTemplate.convertAndSend("/topic/admin/alerts", alert);
    }

    public void publishLifecycle(ElectionLifecycleNotification notification) {
        messagingTemplate.convertAndSend("/topic/elections/lifecycle", notification);
    }

    public void publishUserNotification(String username, Object payload) {
        messagingTemplate.convertAndSend("/topic/users/" + username + "/notifications", payload);
    }
}

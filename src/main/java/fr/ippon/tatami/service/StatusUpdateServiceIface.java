package fr.ippon.tatami.service;

import java.util.Collection;

import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.service.exception.ArchivedGroupException;
import fr.ippon.tatami.service.exception.ReplyStatusException;

public interface StatusUpdateServiceIface {

    public abstract void postStatus(String content, boolean statusPrivate, Collection<String> attachmentIds);

    public abstract void postStatusToGroup(String content, Group group, Collection<String> attachmentIds);

    public abstract void postStatusAsUser(String content, User user);

    public abstract void replyToStatus(String content, String replyTo) throws ArchivedGroupException,
            ReplyStatusException;

}
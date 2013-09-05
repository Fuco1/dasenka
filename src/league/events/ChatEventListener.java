package league.events;

import league.wrappers.ClientWrapper;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public interface ChatEventListener {

    void handlePublicMsgEvent(String channel, String userNick, String text, ClientWrapper app);

    void handlePrivateMsgEvent(String userNick, String text, ClientWrapper app);

    void handleChannelJoinEvent(String channel, String userNick, ClientWrapper app);

    void handleChannelPartEvent(String channel, String userNick, ClientWrapper app);

    void handleUserAuthEvent(String userNick, ClientWrapper app);

    void handleUserAuthorisation(String userNick, ClientWrapper app);
}
